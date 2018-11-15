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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mail;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.GatewayConfiguration;
import org.nhindirect.gateway.smtp.NotificationProducer;
import org.nhindirect.gateway.smtp.NotificationSettings;
import org.nhindirect.gateway.smtp.ReliableDispatchedNotificationProducer;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.FailedDeliveryDSNCreator;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This mailet override the built in Apache James LocalDelivery mailet and sends an MDN dispatched message on successful delivery to a local mailbox
 * if the message request timely and reliable message delivery.
 * In addition, it also sends a DSN failure message if the message cannot be placed into the local mailbox.
 * @author Greg Meyer
 * @since 2.0
 */
public class TimelyAndReliableLocalDelivery extends AbstractNotificationAwareMailet
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TimelyAndReliableLocalDelivery.class);

	protected static final String DISPATCHED_MDN_DELAY = "DispatchedMDNDelay";
	
	private UsersRepository usersRepository;

	private MailboxManager mailboxManager;

	private MetricFactory metricFactory;
	
	protected LocalDelivery localDeliveryMailet;
		
	protected NotificationProducer notificationProducer;
	
	protected int dispatchedMDNDelay;
	
	static
	{		
		initJVMParams();
	}
	
	private synchronized static void initJVMParams()
	{
		/*
		 * Mailet configuration parameters
		 */
		final Map<String, String> JVM_PARAMS = new HashMap<String, String>();
		JVM_PARAMS.put(DISPATCHED_MDN_DELAY, "org.nhindirect.gateway.smtp.james.mailet.DispatchedMDNDelay");
		
		OptionsManager.addInitParameters(JVM_PARAMS);
	}	
	
    @Inject
    public TimelyAndReliableLocalDelivery(UsersRepository usersRepository, @Named("mailboxmanager") MailboxManager mailboxManager,
                         MetricFactory metricFactory) 
    {
        this.metricFactory = metricFactory;
        this.usersRepository = usersRepository;
        this.mailboxManager = mailboxManager;
		// create an instance of the local delivery if we can
		this.localDeliveryMailet = createLocalDeliveryClass();
    }	
	
	/**
	 * {@inheritDoc}
	 */
	public void init() throws MessagingException
	{
		super.init();
		

		
		try
		{
			final String sDispatchedDelay =  GatewayConfiguration.getConfigurationParam(DISPATCHED_MDN_DELAY,
					this, "0"); 
			
			try
			{
				dispatchedMDNDelay = Integer.valueOf(sDispatchedDelay).intValue();
			}
			catch (NumberFormatException e)
			{
				// in case of parsing exceptions
				dispatchedMDNDelay = 0;
			}
			
			localDeliveryMailet.init(this.getMailetConfig());

		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to initialize TimelyAndReliableLocalDelivery.", e);
		}
		
		notificationProducer = new ReliableDispatchedNotificationProducer(new NotificationSettings(true, "Local Direct Delivery Agent", "Your message was successfully dispatched."));
	}
	
	
	protected LocalDelivery createLocalDeliveryClass()
	{
		final LocalDelivery retVal = new LocalDelivery(usersRepository, mailboxManager, metricFactory);
		
		return retVal;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void service(Mail mail) throws MessagingException 
	{
		LOGGER.debug("Calling timely and reliable service method.");
		
		boolean deliverySuccessful = false;
		
		final MimeMessage msg = mail.getMessage();
		final boolean isReliableAndTimely = TxUtil.isReliableAndTimelyRequested(msg);
		
		final SMTPMailMessage smtpMailMessage = mailToSMTPMailMessage(mail);
		
		final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMailMessage);
		
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMailMessage);
		
		
		try
		{
			localDeliveryMailet.service(mail);
			deliverySuccessful = true;
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to invoke service method.", e);
		}
		
		final Tx txToTrack = this.getTxToTrack(msg, sender, recipients);
		
		if (deliverySuccessful)
		{	
			if (isReliableAndTimely && txToTrack.getMsgType() == TxMessageType.IMF)
			{

				// send back an MDN dispatched message
				final Collection<NotificationMessage> notifications = 
						notificationProducer.produce(new Message(msg), recipients.toInternetAddressCollection());
				if (notifications != null && notifications.size() > 0)
				{
					LOGGER.debug("Sending MDN \"dispatched\" messages");
					// create a message for each notification and put it on James "stack"
					for (NotificationMessage message : notifications)
					{
						try
						{
							message.saveChanges();
							
							if (dispatchedMDNDelay > 0)
								Thread.sleep(dispatchedMDNDelay);
							
							getMailetContext().sendMail(message);
						}
						///CLOVER:OFF
						catch (Throwable t)
						{
							// don't kill the process if this fails
							LOGGER.error("Error sending MDN dispatched message.", t);
						}
						///CLOVER:ON
					}
				}
			}
		}
		else
		{
			// create a DSN message regarless if timely and reliable was requested
			if (txToTrack != null && txToTrack.getMsgType() == TxMessageType.IMF)
				this.sendDSN(txToTrack, recipients, false);
		}
		
		LOGGER.debug("Exiting timely and reliable service method.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DSNCreator createDSNGenerator() 
	{
		return new FailedDeliveryDSNCreator(this);
	}
}
