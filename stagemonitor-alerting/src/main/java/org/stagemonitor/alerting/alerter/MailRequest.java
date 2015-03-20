package org.stagemonitor.alerting.alerter;


import static org.stagemonitor.core.util.StringUtils.isEmpty;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.stagemonitor.core.util.Assert;


/**
 * This class represents a mail object, which contains all relevant information
 * to create and send a mail.
 */
public class MailRequest {

	private String subject, from;

	private String recipients;

	private String htmlPart;

	private String textPart;

	/**
	 * Creates a new {@link MailRequest}
	 * <p>
	 * Caution: If the parameter values contain empty or null values an
	 * {@link IllegalArgumentException} will be thrown.
	 * <p>
	 * Add Content using the request, e.g.:
	 * <pre>
	 * MailRequest mailRequest = new MailRequest(subject, from, to).html(htmlPart).text(textPart);
	 * </pre>
	 *
	 * @param subject
	 *            The email subject
	 * @param from
	 *            Sender address of the desired mail
	 * @param recipients
	 *            Multiple recipients for this mail
	 */
	public MailRequest(String subject, String from, String recipients) {
		Assert.hasText(subject, "Missing email subject");
		Assert.hasText(from, "Missing email sender address");
		Assert.hasText(recipients, "Missing email recipients");
		this.subject = subject;
		this.from = from;
		this.recipients = recipients;
	}

	public MailRequest htmlPart(String htmlPart) {
		this.htmlPart = htmlPart;
		return this;
	}

	public MailRequest textPart(String textPart) {
		this.textPart = textPart;
		return this;
	}

	/**
	 * Creates a MimeMessage containing given Multipart.
	 * Subject, sender and content and session will be set.
	 * @param session current mail session
	 * @return MimeMessage without recipients
	 * @throws MessagingException
	 */
	public MimeMessage createMimeMessage(Session session) throws MessagingException {
		if (isEmpty(htmlPart) && isEmpty(textPart)) {
			throw new IllegalArgumentException("Missing email content");
		}
		final MimeMessage msg = new MimeMessage(session);
		msg.setSubject(subject);
		msg.setFrom(new InternetAddress(from));
		msg.setContent(createMultiPart());
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients, false));
		return msg;
	}

	/**
	 * Creates a Multipart from present parts.
	 * @return multipart with all present parts
	 * @throws MessagingException
	 */
	private Multipart createMultiPart() throws MessagingException {
		Multipart multipart = new MimeMultipart("alternative");
		if (textPart != null) {
			// add text first, to give priority to html
			multipart.addBodyPart((BodyPart) createTextMimePart());
		}
		if (htmlPart != null) {
			multipart.addBodyPart((BodyPart) createHtmlMimePart());
		}
		return multipart;
	}

	/**
	 * Creates a MimePart from HTML part.
	 *
	 * @return mimePart from HTML part
	 * @throws MessagingException
	 */
	private MimePart createHtmlMimePart() throws MessagingException {
		MimePart bodyPart = new MimeBodyPart();
		bodyPart.setContent(htmlPart, "text/html; charset=utf-8");
		return bodyPart;
	}

	/**
	 * Creates a MimePart from text part.
	 *
	 * @return mimePart from HTML string
	 * @throws MessagingException
	 */
	private MimePart createTextMimePart() throws MessagingException {
		MimePart bodyPart = new MimeBodyPart();
		bodyPart.setText(textPart);
		return bodyPart;
	}

}
