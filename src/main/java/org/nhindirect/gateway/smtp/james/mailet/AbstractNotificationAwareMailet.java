/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.gateway.smtp.james.mailet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.impl.NoOpTxServiceClient;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Abstract mailet class that instantiates instances of the monitoring service, parser, and DSNCreator.  Also include utility methods for retrieving the message sender and 
 * recipients based on the SMTP envelope (if available) and parsing messages into monitoring Tx objects.
 * @author Greg Meyer
 * @Since 2.0
 */
public abstract class AbstractNotificationAwareMailet extends GenericMailet
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNotificationAwareMailet.class);	
	
	
	protected ApplicationContext ctx;
	protected DSNCreator dsnCreator;
	protected TxDetailParser txParser;
	protected TxService txService;	
	
	/**
	 * {@inheritDoc}
	 * Creates the monitoring service and DSNCreator.
	 */
	public void init() throws MessagingException
	{
		super.init();

		ctx = createSpringApplicationContext();
		
		txParser = createTxDetailParser();
		
		// create the Tx services
		txService = createTxServices();
		
		// create the DSN creator
		dsnCreator = createDSNGenerator();

	}
	
	/**
	 * Creates a spring application context.
	 * @return Spring application context.
	 */
	protected ApplicationContext createSpringApplicationContext()
	{
		return null;
	}
	
	/**
	 * Creates the DSNCreator object
	 */
	protected DSNCreator createDSNGenerator()
	{
		if (this.ctx == null)
		{
			LOGGER.warn("Application context is null.  DSN creation will be disabled.");
			return null;
		}
		
		try
		{
			return ctx.getBean(DSNCreator.class);
		}
		catch (Exception e)
		{
			LOGGER.warn("DSN creator not found in application context.  DSN creation will be disabled.");
			return null;
		}			
	
	}
	
	/**
	 * Sends a DSN message using information in a pre-parsed Tx object and a list of failed message recipients.  This builds the DSN message using
	 * the {@link DSNCreator} specified by {@link #getDSNProvider()} and sends the message using the mailet context sendMail method.  This effectively
	 * places the message on top of the mail stack to be processed as a new incoming message.
	 * @param tx Tx object containing information about the original message.
	 * @param undeliveredRecipeints A collection of recipients that could not receive the original message
	 * @deprecated As of 2.0.1 Use {@link #sendDSN(Tx, NHINDAddressCollection, boolean)}.  
	 */
	protected void sendDSN(Tx tx, NHINDAddressCollection undeliveredRecipeints)
	{
		sendDSN(tx, undeliveredRecipeints, true);
	}
	
	/**
	 * Sends a DSN message using information in a pre-parsed Tx object and a list of failed message recipients.  This builds the DSN message using
	 * the {@link DSNCreator} specified by {@link #getDSNProvider()} and sends the message using the mailet context sendMail method.  This effectively
	 * places the message on top of the mail stack to be processed as a new incoming message.
	 * @param tx Tx object containing information about the original message.
	 * @param undeliveredRecipeints A collection of recipients that could not receive the original message
	 * @param useSenderAsPostmaster Indicates if the sender's domain should be used as the postmaster.  This is generally set to true for failed outgoing messages and
	 * false for failed incoming messages.
	 */
	protected void sendDSN(Tx tx, NHINDAddressCollection undeliveredRecipeints, boolean useSenderAsPostmaster)
	{
		try
		{
			if (dsnCreator != null)
			{
				final Collection<MimeMessage> msgs = dsnCreator.createDSNFailure(tx, undeliveredRecipeints, useSenderAsPostmaster);
				if (msgs != null && msgs.size() > 0)
					for (MimeMessage msg : msgs)
						this.getMailetContext().sendMail(msg);
			}
		}
		catch (Throwable e)
		{
			// don't kill the process if this fails
			LOGGER.error("Error sending DSN failure message.", e);
		}
	}
	
	/**
	 * Creates a trackable monitoring object for a message. 
	 * @param msg The message that is being processed
	 * @param sender The sender of the message
	 * @return A trackable Tx object.
	 */
	protected Tx getTxToTrack(MimeMessage msg, NHINDAddress sender, NHINDAddressCollection recipients)
	{		
		return MessageUtils.getTxToTrack(msg, sender, recipients, this.txParser);
	}
	
	/**
	 * Creates the Tx services.  These are pulled from the Spring application context or a default
	 * NoOp instance is created if the application context does not include an instance.
	 */
	protected TxService createTxServices()
	{		
		if (this.ctx == null)
		{
			LOGGER.warn("Application context is null.  Will fall back to the the NoOp message monitor.");
			return new NoOpTxServiceClient();
		}
		
		try
		{
			return ctx.getBean(TxService.class);
		}
		catch (Exception e)
		{
			LOGGER.warn("Monitoring service not found in application context.  Will fall back to the the NoOp message monitor.");
			return new NoOpTxServiceClient();
		}	
	}
	
	protected TxDetailParser createTxDetailParser()
	{
		return new DefaultTxDetailParser();
	}

	/**
	 * Converts an Apache James Mail message to the common SMTPMailMessage object;
	 * @param mail The Apache James smtp message
	 * @return An SMTPMailMessage message instance container information from the Apache James mail object;
	 */
	@SuppressWarnings("unchecked")
	public static SMTPMailMessage mailToSMTPMailMessage(Mail mail) throws MessagingException
	{
		if (mail == null)
			return null;
		
		List<InternetAddress> toAddrs = new ArrayList<>();
		final InternetAddress fromAddr = (mail.getSender() == null) ? null : mail.getSender().toInternetAddress();
		// uses the RCPT TO commands
		final Collection<MailAddress> recips = mail.getRecipients();
		if (recips == null || recips.size() == 0)
		{
			// fall back to the mime message list of recipients
			final Address[] recipsAddr = mail.getMessage().getAllRecipients();
			for (Address addr : recipsAddr)
				toAddrs.add(new NHINDAddress((InternetAddress)addr));
		}
		else
		{
			toAddrs = recips.stream().
					map(toAddr -> new NHINDAddress(toAddr.toInternetAddress())).collect(Collectors.toList());

		}
		
		return new SMTPMailMessage(mail.getMessage(), toAddrs, fromAddr);
	}
}
