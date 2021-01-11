package tccc.bib.transform.factories;

import tccc.bib.transform.functions.HashMaptoCSV;
import tccc.bib.transform.functions.HashMaptoFF;
import tccc.bib.transform.functions.HashMaptoJSON;
import tccc.bib.transform.functions.HashMaptoXML;
import tccc.bib.transform.interfaces.TransformFromHashMap;

public class CreateFromHashMap {
    public static TransformFromHashMap getTransform(String type){

        switch(type){
            case "JSON":
                return new HashMaptoJSON();
            case "CSV":
                return new HashMaptoCSV();
            case "XML":
                return new HashMaptoXML();
            case "FF":
                return new HashMaptoFF();
            default:
                return null;
        }
    }
}
