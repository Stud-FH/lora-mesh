package production;

import model.Node;

public class Production {

    public static void main(String... args) {

        long serialId = (long) (Math.random() * Long.MAX_VALUE); // todo read config
        var meshClient = new E32LoRaMeshClient();
        var pceClient = new HttpPceClient();
        var logger = new HttpLogger();

        new Node(serialId, meshClient, pceClient, logger)
                .wake();
    }
}
