package loader;

import java.util.Arrays;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Thread currentThread = Thread.currentThread();
        ClassLoader l = new clojure.lang.DynamicClassLoader(currentThread.getContextClassLoader());
        currentThread.setContextClassLoader(l);

        String[] dashM = new String[] {"--main", "loader.core"};
        String [] allArgs = new ArrayList<String>(){
                {addAll(Arrays.asList(dashM));
                 addAll(Arrays.asList(args));}
            }.toArray(new String[args.length + 2]);

        clojure.main.main(allArgs);
    }
}
