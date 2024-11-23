package ru.myx.ae3.i3.web.telnet;
import ru.myx.ae3.produce.Produce;
import ru.myx.ae3.status.StatusRegistry;

/**
 * @author myx
 * 
 */
public final class Main {
	
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println( "BOOT: TELNET is being initialized..." );
		StatusRegistry.ROOT_REGISTRY.register( new TelnetStatusProvider() );
		Produce.registerFactory( new TelnetTargetFactory() );
		System.out.println( "BOOT: TELNET OK" );
	}
	
}
