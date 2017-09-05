package org.stagemonitor.core.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonMerger {

	private static final ObjectMapper mapper = JsonUtils.getMapper();
	private static final Merger defaultMerger = new SimpleMerger();

	/**
	 * Merges two documents with naive merging behaviour (takes all keys from source and adds all keys from replacement.
	 * If a field exists in source as well as in replacement, the field from replacement will be used)
	 *
	 * Example: // source
	 * <pre>
	 *     {
	 *         a: "sourceValue"
	 *         b: "sourceValue"
	 *     }
	 * </pre>
	 *
	 * // replacement
	 * <pre>
	 *     {
	 *         a: "replacementValue"
	 *         c: "replacementValue"
	 *     }
	 * </pre>
	 *
	 * Then calling merge(source, replacement) will yield the following result:
	 *
	 * <pre>
	 *     {
	 *         a: "replacementValue" // taken from replacement as it is merged
	 *         b: "sourceValue"      // taken from source as it is only defined there
	 *         c: "replacementValue" // taken from replacement as it is only defined there
	 *     }
	 * </pre>
	 *
	 * @param source      The first object.
	 * @param replacement The second object. Keys not existing in source will be added. Keys existing in source and
	 *                    replacement will be overriden by replacement fields.
	 * @return the merged object
	 */
	public static JsonNode merge(JsonNode source, JsonNode replacement) throws IOException {
		return new MergeStrategy().execute(source, replacement);
	}

	/**
	 * Merges two documents with naive merging behaviour (takes all keys from source and adds all keys from replacement.
	 * If a field exists in source as well as in replacement, the field from replacement will be used)
	 *
	 * @param source        The first object.
	 * @param replacement   The second object. Keys not existing in source will be added. Keys existing in source and
	 *                      replacement will be overriden by replacement fields.
	 * @param mergeStrategy A merge strategy for further customization of merge behaviour.
	 * @see JsonMerger#merge(JsonNode, JsonNode, MergeStrategy)
	 */
	public static JsonNode merge(JsonNode source, JsonNode replacement, MergeStrategy mergeStrategy) throws IOException {
		return mergeStrategy.execute(source, replacement);
	}

	private static Map<String, JsonNode> groupArrayElementsByKey(ArrayNode fields, String key) {
		Map<String, JsonNode> fieldsMap = new HashMap<String, JsonNode>();
		for (int i = 0; i < fields.size(); i++) {
			JsonNode field = fields.get(i);
			fieldsMap.put(field.get(key).asText(), field);
		}
		return fieldsMap;
	}

	private static <T> Set<T> collectIteratorToSet(Iterator<T> iterator) {
		HashSet<T> set = new HashSet<T>();
		for (; iterator.hasNext(); ) {
			T element = iterator.next();
			set.add(element);
		}
		return set;
	}

	private static <T extends JsonNode> T tryDecode(JsonNode encodedNode, T defaultValue, String fieldName) throws IOException {
		if (encodedNode == null || encodedNode.isNull()) {
			return defaultValue;
		}

		if (!encodedNode.isTextual()) {
			throw new IllegalArgumentException("error before decoding " + fieldName + "." +
					" Expected textual or null field, but encountered " + encodedNode.getNodeType());
		}

		JsonNode decodedNode = mapper.readTree(encodedNode.textValue());
		if (!decodedNode.getNodeType().equals(defaultValue.getNodeType())) {
			throw new IllegalArgumentException("error after decoding" + fieldName + "." +
					" Expected decoded type to be " + defaultValue.getNodeType() + ", but encountered " + decodedNode.getNodeType());
		}

		return (T) decodedNode;
	}

	public static MergeStrategy mergeStrategy() {
		return new MergeStrategy();
	}

	public static class MergeStrategy {

		private final Map<String, Merger> merger;
		private Merger defaultMerger;

		private MergeStrategy() {
			merger = new HashMap<String, Merger>();
			defaultMerger = JsonMerger.defaultMerger;
		}

		/**
		 * Handles objects with the following structure:
		 * <pre>
		 *     {
		 *         field: "[{keyName: keyValue, otherAttributeName: otherAttributeValue}]"
		 *     }
		 * </pre>
		 *
		 * (the content of field is encoded as JSON-string)
		 *
		 * The "field"-array may contain multiple objects. Merging takes all objects in both arrays, determines the
		 * object key bey extracting it from the key-attribute and either: 1. takes the object from source if
		 * replacement has no object with the same key 2. takes the object from replacement if both have an object with
		 * the same key.
		 *
		 * The behaviour with more than one object with the same key in one collection is undefined.
		 *
		 * @param field the name of the field under which the encoded array with the objects can be found
		 * @param key   name of the key-field in the objects compared
		 */
		public MergeStrategy encodedArrayWithKey(String field, String key) {
			merger.put(field, new EncodedArrayWithKeyMerger(field, key));
			return this;
		}

		/**
		 * Handles objects with the following structure:
		 *
		 * <pre>
		 *     {
		 *         field: "{key1: value1, key2: value2}"
		 *     }
		 * </pre>
		 *
		 * (the content of field is encoded as JSON-string)
		 *
		 * The "field"-object may contain an object with multiple keys. Merging takes all key-value-pairs from source
		 * and adds all key-value-pairs from replacement. If a key exists in source and replacement, the key-value-pair
		 * from replacement will be used.
		 *
		 * @param field the name of the field under which the encoded objects to merge can be found
		 */
		public MergeStrategy mergeEncodedObjects(String field) {
			merger.put(field, new EncodedObjectMerger(field));
			return this;
		}

		/**
		 * Overrides the naive default merger.
		 *
		 * @param defaultMerger An instance of the Merger-Interface to handle all fields with no special field merger.
		 */
		public MergeStrategy defaultMerger(Merger defaultMerger) {
			this.defaultMerger = defaultMerger;
			return this;
		}

		/**
		 * Registers a new merger for the given field. The merger is only used for the specified field.
		 *
		 * @param field  The field key which should be handled by the merger.
		 * @param merger An instance of the Merger-Interface to handle this field.
		 */
		public MergeStrategy addFieldMerger(String field, Merger merger) {
			this.merger.put(field, merger);
			return this;
		}

		private JsonNode execute(JsonNode source, JsonNode replacement) throws IOException {
			Set<String> fieldNames = new HashSet<String>();
			fieldNames.addAll(collectIteratorToSet(source.fieldNames()));
			fieldNames.addAll(collectIteratorToSet(replacement.fieldNames()));
			ObjectNode result = mapper.createObjectNode();

			for (String fieldName : fieldNames) {

				Merger merger = this.merger.get(fieldName);
				if (merger == null) {
					merger = defaultMerger;
				}

				try {
					result.set(fieldName, merger.merge(source, replacement, fieldName));
				} catch (JsonParseException e) {
					throw e;
				}

			}

			return result;
		}
	}

	public interface Merger {

		/**
		 * Returns a new JsonNode which represents the merging of source with replacement.
		 *
		 * @param source      The JsonNode to use as default.
		 * @param replacement The JsonNode to override the value of default.
		 * @param fieldName   The field name to examine in this merge.
		 * @return The merged field.
		 * @throws IOException Thrown on parsing errors.
		 */
		JsonNode merge(JsonNode source, JsonNode replacement, String fieldName) throws IOException;

	}

	private static class EncodedArrayWithKeyMerger implements Merger {

		private final String fieldName;
		private final String key;

		EncodedArrayWithKeyMerger(String fieldName, String key) {
			this.fieldName = fieldName;
			this.key = key;
		}

		public JsonNode merge(JsonNode source, JsonNode replacement, String fieldName) throws IOException {
			return merge(source, replacement);
		}

		private JsonNode merge(JsonNode source, JsonNode replacement) throws IOException {
			JsonNode replacementNodeEncoded = replacement.get(fieldName);
			if (replacementNodeEncoded != null) {
				HashMap<String, JsonNode> result = new HashMap<String, JsonNode>();
				result.putAll(groupArrayElementsByKey(tryDecode(source.get(fieldName), mapper.createArrayNode(), fieldName), key));
				result.putAll(groupArrayElementsByKey(tryDecode(replacement.get(fieldName), mapper.createArrayNode(), fieldName), key));
				return TextNode.valueOf(mapper.writeValueAsString(result.values()));
			} else {
				return source.get(fieldName);
			}
		}

	}

	private static class EncodedObjectMerger implements Merger {

		private final String fieldName;

		EncodedObjectMerger(String fieldName) {
			this.fieldName = fieldName;
		}

		public JsonNode merge(JsonNode source, JsonNode replacement, String fieldName) throws IOException {
			return merge(source, replacement);
		}

		private JsonNode merge(JsonNode source, JsonNode replacement) throws IOException {
			JsonNode replacementNodeEncoded = replacement.get(fieldName);
			if (replacementNodeEncoded != null) {
				ObjectNode decodedSource = tryDecode(source.get(fieldName), mapper.createObjectNode(), fieldName);
				decodedSource.setAll(tryDecode(replacement.get(fieldName), mapper.createObjectNode(), fieldName));
				return TextNode.valueOf(mapper.writeValueAsString(decodedSource));
			} else {
				return source.get(fieldName);
			}
		}

	}

	private static class SimpleMerger implements Merger {

		@Override
		public JsonNode merge(JsonNode source, JsonNode replacement, String fieldName) {
			if (replacement.has(fieldName)) {
				return replacement.get(fieldName);
			} else {
				return source.get(fieldName);
			}
		}

	}
}
