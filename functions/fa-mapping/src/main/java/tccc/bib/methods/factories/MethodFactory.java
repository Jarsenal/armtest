package tccc.bib.methods.factories;

import tccc.bib.methods.functions.*;
import tccc.bib.methods.interfaces.*;
import java.util.LinkedHashMap;



public class MethodFactory {

    static LinkedHashMap<String, Method> myMethods = new LinkedHashMap<>();

    public static Method getMethod(String type){
        if(!myMethods.containsKey(type)){
            myMethods.put(type,createMethod(type));
        }

        return myMethods.get(type);
    }


    private static Method createMethod(String type){
        
        switch(type){
            case "xref": return new CrossReferencing();
            case "qualifier": return new Qualifier();
            case "datetime": return new DatetimeFormatting();
            case "number": return new NumberFormatting();
            case "padding": return new Padding();
            case "substring": return new Substring();
            case "trim": return new Trim();
            case "advancetrim": return new AdvanceTrim();
            case "concat": return new Concat();
            case "tostring": return new ToString();
            case "tonumber": return new ToNumber();
            case "toboolean": return new ToBoolean();
            case "ziplist": return new ZipList();
            case "uniquefilter": return new UniqueFilter();
            case "sum": return new Sum();
            case "default": return new Default();
            case "getfirst": return new GetFirst();
            case "concatlist": return new ConcatList();
            case "joinby": return new JoinBy();
            case "abs": return new Abs();
            default: return null;
        }
    }
}