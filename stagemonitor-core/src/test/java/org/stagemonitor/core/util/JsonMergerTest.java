package org.stagemonitor.core.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.stagemonitor.core.util.JsonMerger.mergeStrategy;

public class JsonMergerTest {

	private ObjectMapper mapper = JsonUtils.getMapper();
	private JsonMerger.MergeStrategy mergeStrategy = mergeStrategy().mergeEncodedObjects("fieldFormatMap").encodedArrayWithKey("fields", "name");

	@Test
	public void mergeDefinitions_AddsKeyFromReplacementToSource() throws Exception {
		// Given
		ObjectNode a = mapper.createObjectNode();
		ObjectNode b = mapper.createObjectNode();

		a.put("a", "a");
		b.put("b", "b");

		// When
		JsonNode resultWithEmptyMergeStrategy = JsonMerger.merge(a, b, mergeStrategy());
		JsonNode resultWithoutOptions = JsonMerger.merge(a, b);

		// Then
		assertThat(resultWithEmptyMergeStrategy).isEqualTo(resultWithoutOptions);
		assertThat(resultWithEmptyMergeStrategy).hasSize(2);
		assertThat(resultWithEmptyMergeStrategy.get("a")).isEqualTo(TextNode.valueOf("a"));
		assertThat(resultWithEmptyMergeStrategy.get("b")).isEqualTo(TextNode.valueOf("b"));
	}

	@Test
	public void mergeDefinitions_ReplacesKeysFromSourceWithReplacement() throws Exception {
		// Given
		ObjectNode a = mapper.createObjectNode();
		ObjectNode b = mapper.createObjectNode();

		a.put("a", "a");
		b.put("a", "b");

		// When
		JsonNode result = JsonMerger.merge(a, b, mergeStrategy());

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get("a")).isEqualTo(TextNode.valueOf("b"));
	}

	@Test
	public void mergeDefinitions_EncodedArrayWithKey_ReplacesFieldsSpecifiedInBoth() throws Exception {
		// Given
		JsonNode a = mapper.readTree("{\"a\": \"[{\\\"name\\\": \\\"key\\\", \\\"value\\\": \\\"source\\\"}, {\\\"name\\\": \\\"somethingElse\\\", \\\"value\\\": \\\"somethingElse\\\"}]\"}");
		JsonNode b = mapper.readTree("{\"a\": \"[{\\\"name\\\": \\\"key\\\", \\\"value\\\": \\\"replacement\\\"}, {\\\"name\\\": \\\"somethingThird\\\", \\\"value\\\": \\\"somethingThird\\\"}]\"}");

		// When
		JsonNode result = JsonMerger.merge(a, b, mergeStrategy().encodedArrayWithKey("a", "name"));

		// Then
		assertThat(result).hasSize(1);
		JsonNode mergedArray = mapper.readTree(result.get("a").textValue());
		assertThat(mergedArray).containsExactlyInAnyOrder(
				mapper.createObjectNode().put("name", "key").put("value", "replacement"),
				mapper.createObjectNode().put("name", "somethingElse").put("value", "somethingElse"),
				mapper.createObjectNode().put("name", "somethingThird").put("value", "somethingThird")
		);
	}

	@Test
	public void mergeDefinitions_EncodedArrayWithKey_RetainsFieldsOnlySpecifiedInSource() throws Exception {
		// Given
		JsonNode a = mapper.readTree("{\"a\": \"[{\\\"name\\\": \\\"key\\\", \\\"value\\\": \\\"source\\\"}]\"}");
		JsonNode b = mapper.readTree("{\"a\": \"[]\"}");

		// When
		JsonNode result = JsonMerger.merge(a, b, mergeStrategy().encodedArrayWithKey("a", "name"));

		// Then
		assertThat(result).hasSize(1);
		JsonNode mergedArray = mapper.readTree(result.get("a").textValue());
		assertThat(mergedArray).containsExactlyInAnyOrder(
				mapper.createObjectNode().put("name", "key").put("value", "source")
		);
	}

	@Test
	public void mergeDefinitions_EncodedObjects_ReplacesFieldsSpecifiedInBoth() throws Exception {
		// Given
		JsonNode a = mapper.readTree("{\"a\": \"{\\\"a1\\\": \\\"a1source\\\",\\\"a2\\\": \\\"a2source\\\"}\"}");
		JsonNode b = mapper.readTree("{\"a\": \"{\\\"b1\\\": \\\"b1replacement\\\",\\\"a1\\\": \\\"a1replacement\\\"}\"}");

		// When
		JsonNode result = JsonMerger.merge(a, b, mergeStrategy().mergeEncodedObjects("a"));
		assertThat(result).hasSize(1);

		// Then
		JsonNode encodedObject = mapper.readTree(result.get("a").textValue());
		assertThat(encodedObject).hasSize(3);
		assertThat(encodedObject.get("a1").textValue()).isEqualTo("a1replacement");
		assertThat(encodedObject.get("a2").textValue()).isEqualTo("a2source");
		assertThat(encodedObject.get("b1").textValue()).isEqualTo("b1replacement");
	}

	@Test
	public void mergeDefinitions_EncodedObjects_RetainsFieldsOnlySpecifiedInSource() throws Exception {
		// Given
		JsonNode a = mapper.readTree("{\"a\": \"{\\\"a1\\\": \\\"a1source\\\",\\\"a2\\\": \\\"a2source\\\"}\"}");
		JsonNode b = mapper.readTree("{\"a\": \"{}\"}");

		// When
		JsonNode result = JsonMerger.merge(a, b, mergeStrategy().mergeEncodedObjects("a"));
		assertThat(result).hasSize(1);

		// Then
		JsonNode encodedObject = mapper.readTree(result.get("a").textValue());
		assertThat(encodedObject).hasSize(2);
		assertThat(encodedObject.get("a1").textValue()).isEqualTo("a1source");
		assertThat(encodedObject.get("a2").textValue()).isEqualTo("a2source");
	}

	@Test
	public void mergeDefinitions_PreservesFieldsNotDefinedInReplacement() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"foo\": \"bar\", \"fields\": \"[{\\\"name\\\":\\\"sourceField\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"sourceFieldFormatMap\\\"}}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[]\", \"fieldFormatMap\": \"{}\"}");

		// When
		JsonNode resultDefinitions = JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy);

		// Then
		assertThat(resultDefinitions.get("foo").textValue()).isEqualTo("bar");

		assertThat(mapper.readTree(resultDefinitions.get("fields").textValue()))
				.isEqualTo(mapper.readTree(sourceDefinitions.get("fields").textValue()));

		assertThat(mapper.readTree(resultDefinitions.get("fieldFormatMap").textValue()))
				.isEqualTo(mapper.readTree(sourceDefinitions.get("fieldFormatMap").textValue()));
	}

	@Test
	public void mergeDefinitions_AddsFieldsDefinedInReplacement() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"sourceField\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"sourceFieldFormatMap\\\"}}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"stagemonitorField\\\"}]\", \"fieldFormatMap\": \"{\\\"stagemonitor\\\": {\\\"name\\\": \\\"stagemonitorFieldFormatMap\\\"}}\"}");

		// When
		JsonNode resultDefinitions = JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy);

		// Then
		JsonNode resultFields = mapper.readTree(resultDefinitions.get("fields").textValue());
		assertThat(resultFields).containsExactlyInAnyOrder(
				mapper.createObjectNode().put("name", "sourceField"),
				mapper.createObjectNode().put("name", "stagemonitorField"));

		JsonNode resultFieldFormat = mapper.readTree(resultDefinitions.get("fieldFormatMap").textValue());
		assertThat(resultFieldFormat).hasSize(2);
		assertThat(resultFieldFormat.get("foo")).isEqualTo(mapper.createObjectNode().put("name", "sourceFieldFormatMap"));
		assertThat(resultFieldFormat.get("stagemonitor")).isEqualTo(mapper.createObjectNode().put("name", "stagemonitorFieldFormatMap"));
	}

	@Test
	public void mergeDefinitions_UpdatesFieldsDefinedInReplacementAndSource() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"old\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooOld\\\"}}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// When
		JsonNode resultDefinitions = JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy);

		// Then
		JsonNode resultFields = mapper.readTree(resultDefinitions.get("fields").textValue());
		assertThat(resultFields).containsExactlyInAnyOrder(
				mapper.createObjectNode().put("name", "fooField").put("value", "new"));

		JsonNode resultFieldFormat = mapper.readTree(resultDefinitions.get("fieldFormatMap").textValue());
		assertThat(resultFieldFormat).hasSize(1);
		assertThat(resultFieldFormat.get("foo")).isEqualTo(mapper.createObjectNode().put("name", "fooNew"));
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldsDefinition() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": false, \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooOld\\\"}}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatIllegalArgumentException().isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessage("error before decoding fields. Expected textual or null field, but encountered BOOLEAN");
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldsType() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"{}\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooOld\\\"}}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatIllegalArgumentException().isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessage("error after decodingfields. Expected decoded type to be ARRAY, but encountered OBJECT");
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldsJson() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"invalid\", \"fieldFormatMap\": \"{}\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessageContaining("Unrecognized token 'invalid': was expecting ('true', 'false' or 'null')");
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldFormatMapDefinition() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"[]\", \"fieldFormatMap\": false}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatIllegalArgumentException().isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessage("error before decoding fieldFormatMap. Expected textual or null field, but encountered BOOLEAN");
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldFormatMapType() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"[]\", \"fieldFormatMap\": \"[]\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatIllegalArgumentException().isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessage("error after decodingfieldFormatMap. Expected decoded type to be OBJECT, but encountered ARRAY");
	}

	@Test
	public void mergeDefinitions_ThrowsIllegalArgumentExceptionOnInvalidFieldFormatJson() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{\"fields\": \"[]\", \"fieldFormatMap\": \"invalid\"}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"fooField\\\", \\\"value\\\": \\\"new\\\"}]\", \"fieldFormatMap\": \"{\\\"foo\\\": {\\\"name\\\": \\\"fooNew\\\"}}\"}");

		// Then
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy))
				.withMessageContaining("Unrecognized token 'invalid': was expecting ('true', 'false' or 'null')");
	}

	@Test
	public void mergeDefinitions_AddsFieldsDefinedInReplacement_IfNoDefinitionIsPresent() throws Exception {
		// Given
		JsonNode sourceDefinitions = mapper.readTree("{}");
		JsonNode stagemonitorDefinitions = mapper.readTree("{\"fields\": \"[{\\\"name\\\": \\\"stagemonitorField\\\"}]\", \"fieldFormatMap\": \"{\\\"stagemonitor\\\": {\\\"name\\\": \\\"stagemonitorFieldFormatMap\\\"}}\"}");

		// When
		JsonNode resultDefinitions = JsonMerger.merge(sourceDefinitions, stagemonitorDefinitions, mergeStrategy);

		// Then
		JsonNode resultFields = mapper.readTree(resultDefinitions.get("fields").textValue());
		assertThat(resultFields).containsExactlyInAnyOrder(
				mapper.createObjectNode().put("name", "stagemonitorField"));

		JsonNode resultFieldFormat = mapper.readTree(resultDefinitions.get("fieldFormatMap").textValue());
		assertThat(resultFieldFormat).hasSize(1);
		assertThat(resultFieldFormat.get("stagemonitor")).isEqualTo(mapper.createObjectNode().put("name", "stagemonitorFieldFormatMap"));
	}

	@Test
	public void defaultMerger_IsUsedForAllFields() throws Exception {
		// Given
		ObjectNode a = mapper.createObjectNode();
		ObjectNode b = mapper.createObjectNode();

		a.put("a", "a");
		b.put("a", "b");

		// When
		JsonNode result = JsonMerger.merge(a, b,
				mergeStrategy().defaultMerger((source, replacement, fieldName) -> source.get(fieldName)));

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get("a")).isEqualTo(TextNode.valueOf("a"));
	}

	@Test
	public void addFieldMerger_IsUsedForSpecifiedFields() throws Exception {
		// Given
		ObjectNode a = mapper.createObjectNode();
		ObjectNode b = mapper.createObjectNode();

		a.put("a", "source");
		a.put("b", "source");
		b.put("a", "replacement");
		b.put("b", "replacement");

		// When
		JsonNode result = JsonMerger.merge(a, b,
				mergeStrategy().addFieldMerger("b", (source, replacement, fieldName) -> source.get(fieldName)));

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get("a")).isEqualTo(TextNode.valueOf("replacement"));
		assertThat(result.get("b")).isEqualTo(TextNode.valueOf("source"));
	}


}
