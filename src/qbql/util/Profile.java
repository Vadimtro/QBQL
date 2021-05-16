package qbql.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class Profile {

    public static void sample( final int duration, final int granularity ) {
        final String threadName =  Thread.currentThread().getName();
        new Thread() {
            public void run() {
                final long start = System.currentTimeMillis();
                while( System.currentTimeMillis() < start + duration ) 
                    try {
                        Thread.sleep(granularity);

                        ThreadMXBean bean = ManagementFactory.getThreadMXBean(); 
                        ThreadInfo[] infos = bean.dumpAllThreads(true, true); 

                        System.out.println("================="+(System.currentTimeMillis()-start)); 
                        for ( ThreadInfo info : infos ) {
                            //if( !threadName.equals(info.getThreadName()))
                                //continue;
                            //System.out.println("----"+info.getThreadName()+"----");        
                            StackTraceElement[] elems = info.getStackTrace();
                            for (int i = 0; i < 4 && i < elems.length; i++) 
                                System.out.println(elems[i].toString());
                        }
                    } catch ( InterruptedException e ) {
                    }
            }
        }.start();
    }
}
