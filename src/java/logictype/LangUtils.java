package logictype;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author oper
 */
public class LangUtils {

    public static List<String> newstringlist(String... theStringsAsVariadic) {
	LinkedList<String> alloc = new LinkedList<>();
	for (int i = 0, j = theStringsAsVariadic.length; i < j; i++)
	    alloc.add(theStringsAsVariadic[i]);
	return alloc;
    }

    public static List<Object> newlist(Object... theElementsAsVariadic) {
	LinkedList<Object> alloc = new LinkedList<>();
	for (int i = 0, j = theElementsAsVariadic.length; i < j; i++)
	    alloc.add(theElementsAsVariadic[i]);
	return alloc;
    }
    
}
