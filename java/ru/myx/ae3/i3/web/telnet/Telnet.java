/*
 * Created on 02.06.2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.ae3.i3.web.telnet;

import ru.myx.ae3.Engine;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.report.ReportReceiver;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
class Telnet {
	static final long			STARTED			= Engine.fastTime();
	
	static final ReportReceiver	LOG				= Report.createReceiver( "ae3.proto-telnet" );
	
	final static ExecProcess	CTX				= Exec.createProcess( Exec.getRootProcess(), "ANSI telnet interface" );
	
	static final String			PNAME_TELNET	= "TELNET";
	
	static {
		Telnet.LOG
				.event( "INFO",
						"#LEGEND#",
						"CODE\tPROTOCOL\tCOMMAND\tMEAN-ADDRESS\tPEER-ADDRESS\tK-ALIVE\tCOMPRESS\tCHUNKED\tC-LENGTH\tSERVED\tSENT\tPEER-ID\tURL" );
		
	}
}
