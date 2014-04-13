/**
 * 
 */
package org.jocean.event.extend.demo;

import java.util.Random;

import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.EventReceiver;
import org.jocean.event.api.EventReceiverSource;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.event.extend.Runners;
import org.jocean.event.extend.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author hp
 *
 */


public class HelloDemo {

    private static final Logger LOG = 
    		LoggerFactory.getLogger(HelloDemo.class);

    public class DemoFlow extends AbstractFlow<DemoFlow> {
        final BizStep INIT = new BizStep("INIT")
                .handler( selfInvoker("onCoin") )
                .handler( selfInvoker("onPass") )
                .freeze();
        
        private final BizStep LOCKED = 
        		new BizStep("LOCKED")
        		.handler( selfInvoker("onCoin") )
        		.freeze();
        		
        private final BizStep UNLOCKED = 
        		new BizStep("UNLOCKED")
        		.handler( selfInvoker( "onPass") )
        		.freeze();

		@OnEvent(event="coin")
		private BizStep onCoin() {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
			LOG.info("{}: state({}) accept {}", 
				selfEventReceiver(), currentEventHandler().getName(),  currentEvent() );
			
	        return ((BizStep)this.fireDelayEventAndPush(
		        UNLOCKED.delayEvent(selfInvoker("onTimeout"))
                    .args("hello", "world")
                    .delayMillis( (Math.random() > 0.5f) ? 10000L : 5000L))
                    .owner()).freeze();
		}
		
		@OnEvent(event="pass")
		private BizStep onPass() throws Exception {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
            LOG.info("{}: state({}) accept {}", 
                    selfEventReceiver(), currentEventHandler().getName(),  currentEvent() );
            
            this.popAndCancelDealyEvents();
            
			return LOCKED;
		}
		
		@SuppressWarnings("unused")
        private BizStep onTimeout(final String arg1, final String arg2) {
			System.out.println("handler:" + currentEventHandler() + ",event:" + currentEvent());
			LOG.info("{}: {} accept timeout[{}], args 1.{} 2.{}", 
		            selfEventReceiver(), currentEventHandler().getName(),  currentEvent(),
					arg1, arg2 );
			setEndReason( "timeout" );
			return null;
		}

        @Override
        public String toString() {
            return "DemoFlow []";
        }
    }
    
	private void run() throws Exception {
        final EventReceiverSource source = 
        		Runners.build(new Runners.Config()
        			.objectNamePrefix("demo:type=test")
		        	.name("demo")
		        	.timerService(Services.lookupOrCreateTimerService("demo"))
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
