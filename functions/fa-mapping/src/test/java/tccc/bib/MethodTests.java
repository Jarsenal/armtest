package tccc.bib;

import org.junit.jupiter.api.Test;

import tccc.bib.methods.functions.*;
import tccc.bib.tools.IndexTracker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.AbstractList;
import java.util.logging.Logger;
import com.microsoft.azure.functions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;


/**
 * Unit test for Function class.
 */
public class MethodTests {
    /**
     * Unit test for HttpTriggerJava method.
     */
    @Test
    public void testConcat() throws Exception {
        // Setup Maps
        LinkedHashMap<String, Object> concat = new LinkedHashMap<>();
        concat.put("string","Add");
        concat.put("direction","right");

        LinkedHashMap<String, Object> request = new LinkedHashMap<>();
        request.put("field1","yes");
        request.put("field2","no");

        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("concat",concat);

        // init methods
        Concat concatMethod = new Concat();

        // Call Method
        String concatResult = (String)concatMethod.run("Test", request, null, null, null, config, new IndexTracker(), null);

        // Verify
        String expected = "TestAdd";
        assertTrue(concatResult.equals(expected),String.format("%s doee not match %s",concatResult,expected));
    }

    @Test
    public void testConcatNull() throws Exception {
        // Setup Maps
        LinkedHashMap<String, Object> concat = new LinkedHashMap<>();
        concat.put("string","$$.field3");
        concat.put("direction","right");

        LinkedHashMap<String, Object> request = new LinkedHashMap<>();
        request.put("field1","yes");
        request.put("field2","no");

        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("concat",concat);

        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        Concat concatMethod = new Concat();

        // Call Method
        String concatResult = (String)concatMethod.run("Test", request, null, null, null, config, new IndexTracker(),context);

        // Verify
        String expected = "Test";
        assertTrue(concatResult.equals(expected),String.format("%s doee not match %s",concatResult,expected));
    }

    @Test
    public void testQualifier() throws Exception {
        
        LinkedHashMap<String, Object> qualif = new LinkedHashMap<>();
        qualif.put("key","$$.key");
        qualif.put("value","1");
        qualif.put("return","$$.value");

        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("qualifier",qualif);


        LinkedList<Object> request = new LinkedList<>();
        
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","yes");
        request.add(node);
        
        node = new LinkedHashMap<>();
        node.put("key","2");
        node.put("value","no");
        request.add(node);
        
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        Qualifier qualMethod = new Qualifier();

        // Call Method
        String result = (String)qualMethod.run(request, "$$", null, null, null, config,  new IndexTracker(), context);

        // Verify
        String expected = "yes";
        assertTrue(result.equals(expected),String.format("%s does not match %s",result,expected));
    }

    @Test
    public void testQualifierList() throws Exception {
        
        LinkedHashMap<String, Object> qualif = new LinkedHashMap<>();
        qualif.put("key","$$.key");
        qualif.put("value","1");
        qualif.put("return","$$.value");

        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("qualifier",qualif);


        LinkedList<Object> request = new LinkedList<>();
        
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","yes");
        request.add(node);
        
        node = new LinkedHashMap<>();
        node.put("key","2");
        node.put("value","no");
        request.add(node);
       
        node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","no");
        request.add(node);
       
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        Qualifier qualMethod = new Qualifier();

        // Call Method
        Object result = qualMethod.run(request, "$$", null, null, null, config, new IndexTracker(), context);

        // Verify
        Integer expected = 2;
        assertTrue(result.equals(expected),String.format("%d items does not match %d items in list",((AbstractList)result).size(),expected));
    }


    @Test
    public void testZipList() throws Exception {
        
        LinkedHashMap<String, Object> ziplist = new LinkedHashMap<>();
        ziplist.put("name","_item");
        ziplist.put("list","$$.list2");
        
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("ziplist",ziplist);


        LinkedHashMap<String, Object> request = new LinkedHashMap<>();

        LinkedList<Object> list1 = new LinkedList<>();
        
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","yes");
        list1.add(node);
        
        node = new LinkedHashMap<>();
        node.put("key","2");
        node.put("value","no");
        list1.add(node);

        LinkedList<Object> list2 = new LinkedList<>();
        
        node = new LinkedHashMap<>();
        node.put("key","3");
        node.put("value","yes");
        list2.add(node);
        
        node = new LinkedHashMap<>();
        node.put("key","4");
        node.put("value","no");
        list2.add(node);

        request.put("list1",list1);
        request.put("list2",list2);
        
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        ZipList zipMethod = new ZipList();

        // Call Method
        @SuppressWarnings("unchecked")
        AbstractList<Object> result = (AbstractList<Object>)zipMethod.run(list1, request, null, null, null, config,  new IndexTracker(), context);

        // Verify
        Integer expected = 4;
        assertTrue(result.size() == expected,String.format("%d length does not match expected %d length",result.size(),expected));
    }


    @Test
    public void testUniqueFilter() throws Exception {
        
        LinkedHashMap<String, Object> check = new LinkedHashMap<>();
        check.put("value","$$.key");

        LinkedList<LinkedHashMap<String, Object>> uniquefilter = new LinkedList<>();
        uniquefilter.add(check);
        
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("uniquefilter",uniquefilter);
        
        LinkedList<Object> request = new LinkedList<>();
        
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","yes");
        request.add(node);
        
        node = new LinkedHashMap<>();
        node.put("key","2");
        node.put("value","no");
        request.add(node);

        node = new LinkedHashMap<>();
        node.put("key","1");
        node.put("value","no");
        request.add(node);

        
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        UniqueFilter uniqueMethod = new UniqueFilter();

        // Call Method
        @SuppressWarnings("unchecked")
        AbstractList<Object> result = (AbstractList<Object>)uniqueMethod.run(request, null, null, null, null, config,  new IndexTracker(), context);

        // Verify
        Integer expected = 2;
        assertTrue(result.size() == expected,String.format("%d length does not match expected %d length",result.size(),expected));
    }


    @Test
    public void testtoNumber() throws Exception {
        
        // init
        
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        ToNumber toNumberMethod = new ToNumber();

        // Call Method
        BigDecimal result = (BigDecimal)toNumberMethod.run("1", null, null, null, null, null,  new IndexTracker(), context);

        // Verify
        BigDecimal expected = new BigDecimal(1);
        assertTrue(expected.equals(result),String.format("%s does not match %s",result.toString(),expected.toString()));
    }

    @Test
    public void testtoNumberwithNegativeatend() throws Exception {
        
        // init
        
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        ToNumber toNumberMethod = new ToNumber();

        // Call Method
        BigDecimal result = (BigDecimal)toNumberMethod.run("1-", null, null, null, null, null,  new IndexTracker(), context);

        // Verify
        BigDecimal expected = new BigDecimal(-1);
        assertTrue(expected.equals(result),String.format("%s does not match %s",result.toString(),expected.toString()));
    }

    @Test
    public void testtoNumberList() throws Exception {
        
        // init
        LinkedList<String> list = new LinkedList<>();
        list.add("1");
        list.add("2");
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        ToNumber toNumberMethod = new ToNumber();

        // Call Method
        AbstractList<BigDecimal> result = (AbstractList<BigDecimal>)toNumberMethod.run(list, null, null, null, null, null,  new IndexTracker(), context);

        // Verify
        LinkedList<BigDecimal> expected = new LinkedList<>();
        expected.add(new BigDecimal(1));
        expected.add(new BigDecimal(2));
        
        assertTrue(expected.get(0).equals(result.get(0)),String.format("%s does not match %s",result.get(0).toString(),expected.get(0).toString()));
        assertTrue(expected.get(1).equals(result.get(1)),String.format("%s does not match %s",result.get(1).toString(),expected.get(1).toString()));
    }


    @Test
    public void testSum() throws Exception {
        
        // init
        LinkedList<BigDecimal> list = new LinkedList<>();
        list.add(new BigDecimal(1));
        list.add(new BigDecimal(2));
        // context
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // init methods
        Sum sum = new Sum();

        // Call Method
        BigDecimal result = (BigDecimal)sum.run(list, null, null, null, null, null,  new IndexTracker(), context);

        // Verify
        BigDecimal expected = new BigDecimal(3);
        
        assertTrue(expected.equals(result),String.format("%s does not match %s",result.toString(),expected.toString()));
    }


}
