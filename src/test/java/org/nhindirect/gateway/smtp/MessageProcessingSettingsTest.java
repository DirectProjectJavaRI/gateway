package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.File;


public class MessageProcessingSettingsTest
{

	private static final File validSaveFolder = new File("./target/SaveMessageFolder");
	static final char[] invalidFileName;
	
	static
	{
		invalidFileName = new char[Character.MAX_VALUE];
		
		for (char i = 1; i < Character.MAX_VALUE; ++i)
		{
			invalidFileName[i - 1] = i;
		}
	}
	
	private static class ConcreteMessageProcessingSettings extends MessageProcessingSettings
	{
		
	}
	
	@Test
	public void testConstructor()
	{
		ConcreteMessageProcessingSettings settings = new ConcreteMessageProcessingSettings();
		
		assertNull(settings.getSaveMessageFolder());
	}
	
	@Test
	public void testSetSaveMessageFolder()
	{
		ConcreteMessageProcessingSettings settings = new ConcreteMessageProcessingSettings();
		settings.setSaveMessageFolder(validSaveFolder);
		
		assertEquals(validSaveFolder.getAbsolutePath(), settings.getSaveMessageFolder().getAbsolutePath());
		assertTrue(settings.hasSaveMessageFolder());
	}
	
	@Test
	public void testSetSaveMessageFolder_InvalidFolderName_AssertException()
	{
		
		File invalidFile = new File(new String(invalidFileName));
		
		ConcreteMessageProcessingSettings settings = new ConcreteMessageProcessingSettings();
		
		boolean exceptionOccured = false;
		try
		{
			settings.setSaveMessageFolder(invalidFile);
		}
		catch (IllegalArgumentException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
		
	}	
}
