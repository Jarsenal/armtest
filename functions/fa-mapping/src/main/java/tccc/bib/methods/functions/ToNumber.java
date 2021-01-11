package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;

import java.util.AbstractMap;
import java.util.AbstractList;
import tccc.bib.tools.IndexTracker;

import java.math.BigDecimal;
import java.util.stream.Collectors;



public class ToNumber implements  Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
    {

        if(value != null){
            if(value instanceof AbstractList ) {
                value = ((AbstractList<Object>)value).stream()
                        .map(x -> (getNumber(x)))
                        .collect(Collectors.toList());
            } 
            else {
                value = getNumber(value);
            }
        }
        return value;
    } 

    private BigDecimal getNumber(Object value){
        BigDecimal mynumber = null;
        if(value instanceof java.lang.String){
            String num = (String)value;
            if(num.matches("\\d*\\.?\\d*-")){
                num = num.substring(0,num.length() - 1);
                num = "-".concat(num);
            }
            mynumber = (num.isEmpty()?null:new BigDecimal(num));
        }
        return mynumber;
    }
}