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

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.FailedDeliveryDSNCreator;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Notification messages are not tracked by the security and trust mailet to allow for a modular tracking design.  This mailet should be configured to 
 * follow the security and trust mailet in to ensure DNS and MDN message are properly tracked and monitored.
 * @author Greg Meyer
 * @since 2.0
 */
@Slf4j
public class TrackIncomingNotification extends AbstractNotificationAwareMailet
{
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
		JVM_PARAMS.put(SecurityAndTrustMailetOptions.MONITORING_SERVICE_URL_PARAM, "org.nhindirect.gateway.smtp.james.mailet.TxServiceURL");
		
		OptionsManager.addInitParameters(JVM_PARAMS);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init() throws MessagingException
	{
		super.init();
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void service(Mail mail) throws MessagingException 
	{ 		
		log.debug("Calling track incoming notification service");
		final MimeMessage msg = mail.getMessage();

		final SMTPMailMessage smtpMailMessage = mailToSMTPMailMessage(mail);
		
		final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMailMessage);
		
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMailMessage);
		
		final Tx txToMonitor = getTxToTrack(msg, sender, recipients);
	
		// track message
		if (txToMonitor != null && (txToMonitor.getMsgType() == TxMessageType.DSN || 
				txToMonitor.getMsgType() == TxMessageType.MDN))
		{			
			try
			{
				txService.trackMessage(txToMonitor);
			}
			///CLOVER:OFF
			catch (ServiceException ex)
			{
				log.warn("Failed to submit message to monitoring service.", ex);
			}
			///CLOVER:ON
		}
		
		log.debug("Exiting track incoming notification service");
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DSNCreator createDSNGenerator() 
	{
		return new FailedDeliveryDSNCreator(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ApplicationContext createSpringApplicationContext()
	{
		return new ClassPathXmlApplicationContext("contexts/ConfigServices.xml");
	}	
}
