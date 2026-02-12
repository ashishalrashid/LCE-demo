package LCE;

import java.nio.file.Path;
import java.util.List;

//  This is a demo class to showcase the Public endpoints that are available in the LCEngine
//this covers all the basic needs, for more detail coverage of all the endpoints refer the Readme.given

public class demo {

    public static void main(String[] args) throws Exception {

        System.out.println("=== LexCorpusEngine Demo ===");

        //Create engine (zero config)
        LCEngine engine = new LCEngine();

        // Ingest documents
        engine.ingest(1, "Fast red car drives on the highway");
        engine.ingest(2, "Electric car with fast charging");
        engine.ingest(3, "Red bicycle parked near the car");
        engine.ingest(4, "Fast electric bike with long range");

        //  Search (BM25)
        System.out.println("\nSearch query: \"fast car\"");
        List<Integer> results = engine.search("fast car", 5);
        System.out.println("Results: " + results);

        //  Autocomplete
        System.out.println("\nAutocomplete prefix: \"ca\"");
        System.out.println("Suggestions: " + engine.autocomplete("ca"));

        // Persist index
        Path path = Path.of("data/demo_index.lce");
        engine.save(path);
        System.out.println("\nIndex saved to: " + path);

        //  Load index
        LCEngine loaded = LCEngine.load(path);
        System.out.println("Index loaded successfully");

        // Verify search after load
        System.out.println("\nSearch after load: \"red car\"");
        System.out.println("Results: " + loaded.search("red car", 5));

        // Delete a document
        loaded.DeleteDocument(2);
        System.out.println("\nDeleted document with docId = 2");

        // Verify deletion impact
        System.out.println("Search after deletion: \"fast car\"");
        System.out.println("Results: " + loaded.search("fast car", 5));

        System.out.println("\n=== Demo complete ===");
    }
}
