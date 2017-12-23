package net.coderodde.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class BidirectionalHashMapTest {
    
    private Map<Integer, Integer> map;
    
    @Before
    public void before() {
        map = new BidirectionalHashMap<>();
    }
    
    @Test
    public void testSize() {
        for (int i = 0; i < 10; ++i) {
            assertEquals(i, map.size());
            map.put(i, i);
            assertEquals(i + 1, map.size());
        }
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(map.isEmpty());
        map.remove(4);
        assertTrue(map.isEmpty());
        map.put(1, 1);
        assertFalse(map.isEmpty());
        map.put(2, 2);
        assertFalse(map.isEmpty());
        map.remove(2);
        assertFalse(map.isEmpty());
        map.remove(3);
        assertFalse(map.isEmpty());
        map.remove(1);
        assertTrue(map.isEmpty());
        map.remove(-1);
        assertTrue(map.isEmpty());
    }
    
    @Test
    public void testGet() {
        for (int i = 0; i < 50; ++i) {
            map.put(i, i + 100);
        }
        
        for (int i = 49; i >= 0; --i) {
            assertEquals(Integer.valueOf(i + 100), map.get(i));
        }
        
        for (int i = 50; i < 100; ++i) {
            assertNull(map.get(i));
        }
    }
}
