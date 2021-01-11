package tccc.bib.factories;

import tccc.bib.interfaces.*;
import tccc.bib.tools.RouteSimple;
import tccc.bib.tools.RouteSplit;

import java.util.LinkedHashMap;



public class RoutesFactory {

    static LinkedHashMap<String, IRoute> myroutes = new LinkedHashMap<>();

    public static IRoute getRoute(String type){
        if(!myroutes.containsKey(type)){
            myroutes.put(type, get(type));
        }

        return myroutes.get(type);
    }


    private static IRoute get(String type){
        
        switch(type){
            case "simple": return new RouteSimple();
            case "split": return new RouteSplit();
            default: return null;
        }
    }
}