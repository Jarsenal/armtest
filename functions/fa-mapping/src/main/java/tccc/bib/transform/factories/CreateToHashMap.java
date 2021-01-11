package tccc.bib.transform.factories;

import tccc.bib.transform.functions.CSVtoHashMap;
import tccc.bib.transform.functions.FFtoHashMap;
import tccc.bib.transform.functions.JSONtoHashMap;
import tccc.bib.transform.functions.XMLtoHashMap;
import tccc.bib.transform.interfaces.TransformToHashMap;

public class CreateToHashMap {
    public static TransformToHashMap getTransform(String type){

        switch(type){
            case "JSON":
                return new JSONtoHashMap();
            case "CSV":
                return new CSVtoHashMap();
            case "XML":
                return new XMLtoHashMap();
            case "FF":
                return new FFtoHashMap();
            default:
                return null;
        }
    }
}
