/**
 * 
 */
package org.jocean.seda.demo;

import java.util.Random;

import org.jocean.seda.api.AbstractFlow;
import org.jocean.seda.api.BizStep;
import org.jocean.seda.api.EventHandler;
import org.jocean.seda.api.EventReceiver;
import org.jocean.seda.api.EventReceiverSource;
import org.jocean.seda.api.TimerService;
import org.jocean.seda.api.annotation.OnEvent;
import org.jocean.seda.api.tool.Runners;
import org.jocean.seda.api.tool.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author hp
 *
 */


public class HelloDemo {

    private static final Logger LOG = 
    		LoggerFactory.getLogger(HelloDemo.class);

    private final TimerService timer = Services.lookupOrCreateTimerService("demo");
    
    public class DemoFlow extends AbstractFlow<DemoFlow> {
        final BizStep INIT = new BizStep("INIT")
                .handler( selfInvoker("onCoin") )
                .handler( selfInvoker("onPass") )
                .freeze();
        
        private final BizStep LOCKED = 
        		new BizStep("LOCKED")
        		.handler( selfInvoker("onCoin") )
        		.timer(timer)
        		.freeze();
        		
        private final BizStep UNLOCKED = 
        		new BizStep("UNLOCKED")
        		.handler( selfInvoker( "onPass") )
        		.timer(timer)
        		.freeze();

		@OnEvent(event="coin")
		EventHandler onCoin() {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
			LOG.info("{}: state({}) accept {}", 
				selfEventReceiver(), currentEventHandler().getName(),  currentEvent() );
			return UNLOCKED.bindAndFireDelayedEvent(
						selfEventReceiver(), 
						(Math.random() > 0.5f) ? 100L : 5000L, 
						selfInvoker("onTimeout"),
						BizStep.uniqueEvent( "timer-" ), 
						"hello", "world"
					)
					.rename( /*selfId()+*/"-UNLOCKED")
					.freeze();
		}
		
		@OnEvent(event="pass")
		EventHandler onPass() {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
            LOG.info("{}: state({}) accept {}", 
                    selfEventReceiver(), currentEventHandler().getName(),  currentEvent() );
			((BizStep)currentEventHandler()).cancelAllDelayedEvents();
			return LOCKED;
		}
		
		EventHandler onTimeout(final String arg1, final String arg2) {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
			LOG.info("{}: {} accept timeout[{}], args 1.{} 2.{}", 
		            selfEventReceiver(), currentEventHandler().getName(),  currentEvent(),
					arg1, arg2 );
			setEndReason( "timeout" );
			return null;
		}

    }
    
	private void run() throws Exception {
        final EventReceiverSource source = 
        		Runners.build(new Runners.Config()
        			.objectNamePrefix("demo:type=test")
		        	.name("demo")
		        	.executorSource(Services.lookupOrCreateFlowBasedExecutorSource("demo"))
		    		);
        
        final DemoFlow flow = new DemoFlow();
        final EventReceiver receiver = source.create(flow, flow.INIT);
        
    	while (true) {
    	    final String event = genEvent();
    		final boolean ret = receiver.acceptEvent(event);
    		LOG.debug("acceptEvent {} return value {}", event, ret);
    		
    		Thread.sleep(1000L);
    	}
	}
	
    public static void main(String[] args) throws Exception {
    	new HelloDemo().run();
    }
    
	private static String genEvent() {
		final Random r = new Random();
		
		int i1 = r.nextInt();
		
		return i1 % 2 == 1 ? "coin" : "pass";
	}
	
}
