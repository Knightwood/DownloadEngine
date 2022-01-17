package com.kiylx.download_module.utils.apptools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 创建者 kiylx
 * 创建时间 2020/3/18 18:48
 */
public class HashMapProcess {
    /**
     * @param hashMap
     * @param value   要检索的值
     * @param <K>     hashmap的key的类型
     * @param <T>     hashmap的value的类型
     * @return value对应的pos
     * 获取value在hashMap中的位置
     */
    public static <K, T> int getValuePos(HashMap<K, T> hashMap, T value) {
        Collection<T> values = hashMap.values();
        List<T> valueList = new ArrayList<>(values);
        return valueList.indexOf(value);
    }

    /**
     * @param hashMap
     * @param key     要检索的值
     * @param <K>     hashmap的key的类型
     * @param <T>     hashmap的value的类型
     * @return value对应的pos
     * 获取key在hashMap中的位置
     */
    public static <K, T> int getKeyPos(HashMap<K, T> hashMap, K key) {
        int i = 0;
        Set<K> set = hashMap.keySet();
        Iterator<K> it = set.iterator();
        while (it.hasNext()) {
            if (it.next() != key) {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    /**
     * @param hashMap
     * @param <K>     hashmap的key的类型
     * @param <T>     hashmap的value的类型
     * @return key的list
     */
    public static <K, T> List<K> getKeys(HashMap<K, T> hashMap) {
        Set<K> set = hashMap.keySet();
        List<K> result = new ArrayList<>(set);
        return result;
    }

    /**
     * @param hashMap
     * @param <K>     hashmap的key的类型
     * @param <T>     hashmap的value的类型
     * @return value的list
     */
    public static <K, T> List<T> getValues(HashMap<K, T> hashMap) {
        Collection<T> collection = hashMap.values();
        List<T> list = new ArrayList<>(collection);
        return list;
    }

    /**
     * @param hashMap
     * @param <K>     hashmap的key的类型
     * @param <T>     hashmap的value的类型
     * @return value对应的Key
     * <p>
     * 获取value对应的key值
     */
    public static <K, T> K getKeyOfValues(HashMap<K, T> hashMap, T value) {
        if (hashMap.containsValue(value)){
            int valuePos = getValuePos(hashMap, value);
            return getKeys(hashMap).get(valuePos);
        }else {
            return null;
        }

    }
}
