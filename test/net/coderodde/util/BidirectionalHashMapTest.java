package net.coderodde.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BidirectionalHashMapTest {
    
    private BidirectionalHashMap<Integer, Integer> map;
    
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
    public void testEntrySet1() {
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
    
    @Test
    public void testEntrySet2() {
        map.put(10, 100);
        map.put(3, 30);
        map.put(5, 50);
        map.put(1, 10);
        
        map.put(3, 40);
        
        Iterator<Map.Entry<Integer, Integer>> iterator = 
                map.entrySet().iterator();
        
        Map.Entry<Integer, Integer> e;
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(10), e.getKey());
        assertEquals(Integer.valueOf(100), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(3), e.getKey());
        assertEquals(Integer.valueOf(40), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(5), e.getKey());
        assertEquals(Integer.valueOf(50), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(1), e.getKey());
        assertEquals(Integer.valueOf(10), e.getValue());
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
    
    @Test
    public void testInverseGet() {
        BidirectionalHashMap<Integer, String> map =
                new BidirectionalHashMap<>();
        
        map.put(1, "1");
        map.put(2, "2");
        map.put(4, "4");
        
        assertNull(map.inverseMap().get("a"));
        assertEquals(Integer.valueOf(1), map.inverseMap().get("1"));
        assertEquals(Integer.valueOf(2), map.inverseMap().get("2"));
        assertEquals(Integer.valueOf(4), map.inverseMap().get("4"));
        
        map.put(2, "22");
        
        assertEquals(Integer.valueOf(2), map.inverseMap().get("22"));
        assertNull(map.inverseMap().get("2"));
        assertEquals("22", map.get(2));
    }
    
    @Test
    public void testInversePut() {
        BidirectionalHashMap<Integer, String> map = 
                new BidirectionalHashMap<>();
        
        map.inverseMap().put("1", 1);
        map.inverseMap().put("2", 2);
        
        assertTrue(map.containsValue("1"));
        assertTrue(map.containsValue("2"));
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        
        assertEquals("1", map.get(1));
        assertEquals("2", map.get(2));
        assertNull(map.get(3));
        assertEquals(Integer.valueOf(1), map.inverseMap().get("1"));
        assertEquals(Integer.valueOf(2), map.inverseMap().get("2"));
        assertNull(map.inverseMap().get("3"));
        
        map.inverseMap().put("2", 22);
        assertNull(map.get(2));
        assertEquals("2", map.get(22));
    }
    
    @Test
    public void testInverseRemove() {
        map.put(1, 11);
        map.put(2, 12);
        map.put(4, 14);
        
        map.remove(2);
        assertFalse(map.containsKey(2));
        map.inverseMap().remove(15);
        assertEquals(2, map.size());
        map.inverseMap().remove(11);
        assertFalse(map.containsKey(1));
        assertEquals(1, map.size());
    }
    
    @Test
    public void testOrder() {
        map.put(1, 101);
        map.put(2, 102);
        map.put(3, 103);
        map.put(4, 104);
        map.inverseMap().put(102, -2);
        
        Iterator<Map.Entry<Integer, Integer>> iterator =
                map.entrySet().iterator();
        
        assertTrue(iterator.hasNext());
        Map.Entry<Integer, Integer> e = iterator.next();
        assertEquals(Integer.valueOf(1), e.getKey());
        assertEquals(Integer.valueOf(101), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(3), e.getKey());
        assertEquals(Integer.valueOf(103), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(4), e.getKey());
        assertEquals(Integer.valueOf(104), e.getValue());
        
        assertTrue(iterator.hasNext());
        e = iterator.next();
        assertEquals(Integer.valueOf(-2), e.getKey());
        assertEquals(Integer.valueOf(102), e.getValue());
        assertFalse(iterator.hasNext());
        
        BidirectionalHashMap<String, Integer> map2 = 
                new BidirectionalHashMap<>();
        
        map2.put("1", 1);
        map2.put("2", 2);
        map2.put("3", 3);
        map2.put("4", 4); // (1 -> 2 -> 3 -> 4)
        
        map2.put("1", 5); // (2 -> 3 -> 4 -> 5)
        
        Iterator<Integer> inverseKeySetIterator = 
                map2.inverseMap().keySet().iterator();
        
        assertTrue(inverseKeySetIterator.hasNext());
        Integer i = inverseKeySetIterator.next();
        assertEquals(Integer.valueOf(2), i);
        
        assertTrue(inverseKeySetIterator.hasNext());
        i = inverseKeySetIterator.next();
        assertEquals(Integer.valueOf(3), i);
        
        assertTrue(inverseKeySetIterator.hasNext());
        i = inverseKeySetIterator.next();
        assertEquals(Integer.valueOf(4), i);
        
        assertTrue(inverseKeySetIterator.hasNext());
        i = inverseKeySetIterator.next();
        assertEquals(Integer.valueOf(5), i);
    }
}
