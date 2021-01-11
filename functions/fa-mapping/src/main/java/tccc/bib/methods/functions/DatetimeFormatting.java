package tccc.bib.methods.functions;

import com.microsoft.azure.functions.ExecutionContext;
import tccc.bib.methods.interfaces.Method;
import java.util.Date;
import java.util.Objects;
import java.text.SimpleDateFormat;
import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;

public class DatetimeFormatting implements  Method {

    @SuppressWarnings("unchecked")
    public Object run(Object value,Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception 
            {
                String stod = "";
                String dtos = "";

                if(Objects.isNull(value)) return value;

                if(config.get("datetime") != null){
                    stod = (String)((AbstractMap<String, Object>)config.get("datetime")).get("source");
                    dtos = (String)((AbstractMap<String, Object>)config.get("datetime")).get("target");
                }

                if(!stod.isEmpty()){
                    value = (Date)(new SimpleDateFormat(stod)).parse((String)value);
                }

                if(!dtos.isEmpty()) {
                    value = (new SimpleDateFormat(dtos))
                            .format((Date)value);
                }
                    

                return value;
                        
            }

}