package org.nhindirect.gateway.streams.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.gateway.streams.SmtpRemoteDeliveryInput;
import org.nhindirect.gateway.streams.SmtpRemoteDeliverySource;
import org.nhindirect.stagent.NHINDAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import com.google.common.collect.HashMultimap;

@EnableBinding(SmtpRemoteDeliveryInput.class)
public class SmtpRemoteDeliveryProcessor
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpRemoteDeliveryProcessor.class);	
	
	@Value("${direct.gateway.remotedelivery.gateway.name:}")
	protected String gatewayNames;
	
	@Value("${direct.gateway.remotedelivery.gateway.port:}")
	protected String gatewayPort;
	
	@Value("${direct.gateway.remotedelivery.gateway.username:}")
	protected String gatewayUsername;
	
	@Value("${direct.gateway.remotedelivery.gateway.password:}")
	protected String gatewayPassword;
	
	@Value("${direct.gateway.remotedelivery.gateway.connectionTimeout:}")
	protected String connectionTimeout;
	
	@Value("${direct.gateway.remotedelivery.gateway.startTLS:}")
	protected String startTLS;
	
	@Value("${direct.gateway.remotedelivery.gateway.userSSL:}")
	protected String useSSL;
	
	@Autowired
	protected SmtpRemoteDeliverySource remoteDeliverySource;
	
	@Autowired
	protected ExtendedResolver dnsResolver;
	
	@StreamListener(target = SmtpRemoteDeliveryInput.SMTP_REMOTE_DELIVERY_MESSAGE_INPUT)
	public void remotelyDeliverMessage(Message<?> streamMsg) throws MessagingException
	{
		final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
	
		if (smtpMessage.getRecipientAddresses().isEmpty())
		{
			LOGGER.warn("Mail from {} has no recipients and can not be remotely delivered", smtpMessage.getMailFrom());
			return;
		}
		
		if (!streamMsg.getHeaders().containsKey(SmtpRemoteDeliverySource.REMOTE_DELIVERY_GROUPED))
		{
			LOGGER.debug("Message id {} needs to be grouped.  Grouping by domain and requeuing.",  smtpMessage.getMimeMessage().getMessageID());
			groupAndRequeue(smtpMessage);
		}
		else
		{
			LOGGER.info("SmtpRemoteDeliveryProcessor processing message from {} and message id {} ", smtpMessage.getMailFrom().toString(),
					smtpMessage.getMimeMessage().getMessageID());
			try
			{
				remoteDeliver(smtpMessage);
			}
			catch (Exception e)
			{
				LOGGER.error("Error with remote delivery of message id " + smtpMessage.getMimeMessage().getMessageID(), e);
				throw e;
			}
		}
	}
	
	protected void groupAndRequeue(SMTPMailMessage smtpMessage)
	{
		final Map<String, Collection<InternetAddress>> group = groupByDomain(smtpMessage.getRecipientAddresses());
	     
		for (Collection<InternetAddress> recipAddrs : group.values()) 
		{
			final SMTPMailMessage groupedSMTPMessage = new SMTPMailMessage(smtpMessage.getMimeMessage(), 
					new ArrayList<>(recipAddrs), smtpMessage.getMailFrom());
			
			/*
			 * Put back on the queue grouped together
			 */
			remoteDeliverySource.remoteDelivery(groupedSMTPMessage, true);
		}
	}
	
	protected void remoteDeliver(SMTPMailMessage smtpMessage) throws MessagingException
	{
		final List<String> servers = getMailServers(smtpMessage);
		
		Exception lastError = null;
		
		/*
		 * Iterate through servers and try to send to one
		 */
		for (String server : servers)
		{
			final Transport transport = getTransport(server);
			try
			{
				
				if (!StringUtils.isEmpty(gatewayUsername))
					transport.connect(server, gatewayUsername, gatewayPassword);
				else
					transport.connect();
				
				transport.sendMessage(smtpMessage.getMimeMessage(),  
						smtpMessage.getRecipientAddresses().toArray(new InternetAddress[smtpMessage.getRecipientAddresses().size()]));
				
				LOGGER.info("Sucessfully sent message with id {} from {} to server {} ", smtpMessage.getMimeMessage().getMessageID(),  smtpMessage.getMailFrom(), server);
				
				lastError = null;
				
				break;
			}
			catch (Exception e)
			{
				LOGGER.warn("Failed to send message with id " + smtpMessage.getMimeMessage().getMessageID() + " to server " + server + ": " + e.getMessage());
				lastError = e;
			}
			finally
			{
				try {transport.close();} catch (Exception e) {/* no-op */}
			}
		}
		
		if (lastError != null)
		{
			throw new MessagingException("Failed to send message to any known server ", lastError);
		}
	}
	
	protected List<String> getMailServers(SMTPMailMessage smtpMessage) throws MessagingException
	{
		if (!StringUtils.isEmpty(gatewayNames))
			return Arrays.asList(gatewayNames.split(","));
		
		/*
		 * Lookup MX records from DNS
		 */
		final String domainName = new NHINDAddress(smtpMessage.getRecipientAddresses().get(0)).getHost();
		
		// try the configured servers first
		try
		{
			Lookup lu = new Lookup(new Name(domainName), Type.MX);
			lu.setResolver(dnsResolver); 
			lu.setSearchPath((String[])null);
			
			Record[] retRecords = lu.run();
			
			if (retRecords == null || retRecords.length == 0)
			{
				// try again use an A record.  this is allowed
				lu = new Lookup(new Name(domainName), Type.A);
				lu.setResolver(dnsResolver); 
				lu.setSearchPath((String[])null);
				
				retRecords = lu.run();
			}
			
			if (retRecords == null || retRecords.length == 0)
			{
				LOGGER.warn("Could not find any DNS records for domain " + domainName);
				throw new MessagingException("Could not find any DNS records for domain " + domainName);
			}
			
			final List<String> retVal = new ArrayList<>();
			
			for (Record rec : retRecords)
			{
				switch (rec.getType())
				{
					case Type.MX:
					{
						final MXRecord mxRec = MXRecord.class.cast(rec);
						retVal.add(mxRec.getTarget().toString(true));
						break;
					}
					case Type.A:
					{
						final ARecord aRec = ARecord.class.cast(rec);
						retVal.add(aRec.getAddress().getHostName());
						break;
					}
				}
			}
				
			if (LOGGER.isDebugEnabled())
			{
				final StringBuilder builder = new StringBuilder("Found the following servers for domain " + domainName);
				retVal.forEach(server -> builder.append("\r\n\t").append(server));
				
				LOGGER.debug(builder.toString());
				
			}
			
			return retVal;
		}
		catch (Exception e)
		{
			throw new MessagingException("Error looking up DNS records for domain " + domainName, e);
		}
	}
	
	protected Transport getTransport(String server) throws MessagingException
	{
		final Properties props = new Properties();

		props.setProperty("mail.smtp.host", server);
		
		if (!StringUtils.isEmpty(useSSL))
			props.setProperty("mail.smtp.ssl.enable", useSSL);
		
		if (!StringUtils.isEmpty(startTLS))
			props.setProperty("mail.smtp.starttls.enable", connectionTimeout);
		
		if (!StringUtils.isEmpty(connectionTimeout))
			props.setProperty("mail.smtp.connectiontimeout", connectionTimeout);
			
		if (!StringUtils.isEmpty(gatewayPort))
			props.setProperty("mail.smtp.port", gatewayPort);
		
		if (!StringUtils.isEmpty(gatewayUsername))
		{
			props.setProperty("mail.smtp.auth", "true");	
			props.setProperty("mail.smtp.user", gatewayUsername);				
		}
		
		final Session session = Session.getInstance(props);
		return session.getTransport("smtp");
	}
	
	
    protected Map<String, Collection<InternetAddress>> groupByDomain(Collection<InternetAddress> recipients) 
    {
        final HashMultimap<String, InternetAddress> groupByServerMultimap = HashMultimap.create();
        for (InternetAddress recipient : recipients) 
        {
        	final NHINDAddress addr = new NHINDAddress(recipient);
            groupByServerMultimap.put(addr.getHost(), recipient);
        }
        return groupByServerMultimap.asMap();
    }
}
