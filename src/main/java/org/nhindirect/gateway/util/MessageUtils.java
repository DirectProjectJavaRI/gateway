package org.nhindirect.gateway.util;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.stagent.AddressSource;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.NHINDAgent;
import org.nhindirect.stagent.cryptography.SMIMEStandard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common methods used for inspecting and gathering information about messages
 * @author Greg Meyer
 * @Since 6.0
 */
public class MessageUtils
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtils.class);	
	
	/**
	 * Gets the sender of the message.
	 * @param mail The mail object to get the mail information from.
	 * @return The sender of the message.
	 * @throws MessagingException
	 */
	public static NHINDAddress getMailSender(SMTPMailMessage mail) throws MessagingException
	{
		// get the sender
		final InternetAddress senderAddr = getSender(mail);
		if (senderAddr == null)
			throw new MessagingException("Failed to process message.  The sender cannot be null or empty.");
						
			// not the best way to do this
		return new NHINDAddress(senderAddr, AddressSource.From);
	}
	
	/**
	 * Gets the sender attribute of a Mail message
	 * @param mail The message to retrive the sender from
	 * @return The message sender.
	 */
	public static InternetAddress getSender(SMTPMailMessage mail) 
	{
		InternetAddress retVal = null;
		
		if (mail.getMailFrom() != null)
			retVal = mail.getMailFrom();	
		else
		{
			// try to get the sender from the message
			Address[] senderAddr = null;
			try
			{
				if (mail.getMimeMessage() == null)
					return null;
				
				senderAddr = mail.getMimeMessage().getFrom();
				if (senderAddr == null || senderAddr.length == 0)
					return null;
			}
			catch (MessagingException e)
			{
				return null;
			}
						
			// not the best way to do this
			retVal = (InternetAddress)senderAddr[0];	
		}
	
		return retVal;
	}	
	
	/**
	 * Get the recipients of a message by retrieving the recipient list from the SMTP envelope first, then falling back to the recipients
	 * in the message if the recipients cannot be retrieved from the SMTP envelope.
	 * @param mail The mail object that contains information from the SMTP envelope.
	 * @return Collection of message recipients.
	 * @throws MessagingException
	 */
	public static NHINDAddressCollection getMailRecipients(SMTPMailMessage mail) throws MessagingException
	{
		final NHINDAddressCollection recipients = new NHINDAddressCollection();		
		
		// uses the RCPT TO commands
		final Collection<InternetAddress> recips = mail.getRecipientAddresses();
		if (recips == null || recips.size() == 0)
		{
			// fall back to the mime message list of recipients
			final Address[] recipsAddr = mail.getMimeMessage().getAllRecipients();
			for (Address addr : recipsAddr)
			{
				recipients.add(new NHINDAddress(addr.toString(), (AddressSource)null));
			}
		}
		else
			for (InternetAddress addr : recips)
				recipients.add(new NHINDAddress(addr));

		return recipients;
	}	
	
	/**
	 * Determines if a message is incoming or outgoing based on the domains available in the configured agent
	 * and the sender of the message.
	 * @param msg The message that is being processed.
	 * @param sender The sender of the message.
	 * @param agent The STA that contains the available domains.
	 * @return true if the message is determined to be outgoing; false otherwise
	 */
	public static boolean isOutgoing(MimeMessage msg, NHINDAddress sender, NHINDAgent agent)
	{		
		if (agent == null || agent.getDomains() == null)
			return false;
		
		// if the sender is not from our domain, then is has to be an incoming message
		if (!sender.isInDomain(agent.getDomains()))
			return false;
		else
		{
			// depending on the SMTP stack configuration, a message with a sender from our domain
			// may still be an incoming message... check if the message is encrypted
			if (SMIMEStandard.isEncrypted(msg))
			{
				return false;
			}
		}
		
		return true;
	}	
	
	/**
	 * Creates a trackable monitoring object for a message. 
	 * @param msg The message that is being processed
	 * @param sender The sender of the message
	 * @param recipients The message recipients
	 * @param txParser Parser to extract Tx details from the memssage.
	 * @return A trackable Tx object.
	 */
	public static Tx getTxToTrack(MimeMessage msg, NHINDAddress sender, NHINDAddressCollection recipients, TxDetailParser txParser)
	{		
		if (txParser == null)
			return null;
				
		try
		{	
			
			final Map<String, TxDetail> details = txParser.getMessageDetails(msg);
			
			if (sender != null)
				details.put(TxDetailType.FROM.getType(), new TxDetail(TxDetailType.FROM, sender.getAddress().toLowerCase(Locale.getDefault())));
			if (recipients != null && !recipients.isEmpty())
				details.put(TxDetailType.RECIPIENTS.getType(), new TxDetail(TxDetailType.RECIPIENTS, recipients.toString().toLowerCase(Locale.getDefault())));
			
			
			return new Tx(TxUtil.getMessageType(msg), details);
		}
		///CLOVER:OFF
		catch (Exception e)
		{
			LOGGER.warn("Failed to parse message to Tx object.", e);
			return null;
		}
		///CLOVER:ON
	}	
	
	/**
	 * 
	 * Determine if the recipient has been rejected
	 * 
	 * @param rctpAdd
	 * @param rejectedRecips
	 * @return
	 */
	public static boolean isRcptRejected(InternetAddress rctpAdd, NHINDAddressCollection rejectedRecips)
	{
		for (NHINDAddress rejectedRecip : rejectedRecips)
			if (rejectedRecip.getAddress().equals(rctpAdd.toString()))
				return true;
		
		return false;
	}	
	
	/**
	 * Tracks message that meet the following qualifications
	 * <br>
	 * 1. Outgoing IMF message
	 * @param tx The message to monitor and track
	 * @param isOutgoing Indicates the message direction: incoming or outgoing
	 * @param txService the tracking service
	 */
	@SuppressWarnings("incomplete-switch")
	public static void trackMessage(Tx tx, boolean isOutgoing, TxService txService)
	{
		// only track the following message..
		// 1. Outgoing IMF message
		boolean track = false;
		if (tx != null)
		{
			switch (tx.getMsgType())
			{
				case IMF:
				{
					track = isOutgoing;
					break;
				}
			}
		}
		
		if (track)
		{
			try
			{
				txService.trackMessage(tx);
			}
			catch (ServiceException ex)
			{
				LOGGER.warn("Failed to submit message to monitoring service.", ex);
			}
		}
		
	}	
}
