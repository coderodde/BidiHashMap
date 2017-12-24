package net.coderodde.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
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
    
    @Test
    public void testContainsKey() {
        map.put(1, 11);
        map.put(2, 12);
        
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertFalse(map.containsKey(11));
        assertFalse(map.containsKey(12));
        assertFalse(map.containsKey(0));
        assertFalse(map.containsKey(3));
        
        map.remove(2);
        
        assertTrue(map.containsKey(1));
        assertFalse(map.containsKey(2));
    }
    
    @Test
    public void testContainsValue() {
        map.put(1, 11);
        map.put(2, 12);
        
        assertTrue(map.containsValue(11));
        assertTrue(map.containsValue(12));
        assertFalse(map.containsValue(1));
        assertFalse(map.containsValue(2));
    }
    
    @Test
    public void testPut() {
        for (int i = 10; i < 60; ++i) {
            map.put(i, 2 * i);
        }
        
        for (int i = 0; i < 10; ++i) {
            assertFalse(map.containsKey(i));
        }
        
        for (int i = 10; i < 60; ++i) {
            assertTrue(map.containsKey(i));
            assertEquals(Integer.valueOf(2 * i), map.get(i));
        }
        
        for (int i = 60; i < 100; ++i) {
            assertFalse(map.containsKey(i));
            assertNull(map.get(i));
        }
    }
    
    @Test
    public void testRemove() {
        for (int i = 0; i < 10; ++i) {
            assertNull(map.remove(i));
            assertTrue(map.isEmpty());
        }
        
        for (int i = 0; i < 100; ++i) {
            assertEquals(i, map.size());
            map.put(i, i + 3);
            assertEquals(i + 1, map.size());
        }
        
        for (int i = 99; i >= 0; --i) {
            assertEquals(Integer.valueOf(i + 3), map.remove(i));
        }
    }
    
    @Test
    public void testClear() {
        for (int i = 0; i < 200; ++i) {
            map.put(i, i + 1);
        }
        
        assertEquals(200, map.size());
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }
    
    @Test
    public void testEntrySet() {
        map.put(10, 100);
        map.put(3, 30);
        map.put(5, 50);
        map.put(1, 10);
        
        Set<Map.Entry<Integer, Integer>> entrySet = map.entrySet();
        Iterator<Map.Entry<Integer, Integer>> iterator = entrySet.iterator();
        Map.Entry<Integer, Integer> e;
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(10), e.getKey());
        assertEquals(Integer.valueOf(100), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(3), e.getKey());
        assertEquals(Integer.valueOf(30), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(5), e.getKey());
        assertEquals(Integer.valueOf(50), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(1), e.getKey());
        assertEquals(Integer.valueOf(10), e.getValue());
        
        assertFalse(iterator.hasNext());
        
    }
    
    @Test(expected = ConcurrentModificationException.class)
    public void testEntrySetThrowsOnComodification() {
        map.put(1, 1);
        map.put(2, 2);
        
        Iterator<Map.Entry<Integer, Integer>> iterator =
                map.entrySet().iterator();
        
        iterator.next();
        map.remove(1);
        iterator.next();
    }
}
