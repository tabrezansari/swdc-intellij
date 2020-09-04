package com.softwareco.intellij.plugin.sorting;

import com.softwareco.intellij.plugin.KeystrokeCount;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortKeystrokeCount {
    @Test
    public void test_multi_collection_sort() {
        List<KeystrokeCount> list = new ArrayList<>();
        KeystrokeCount countObj = new KeystrokeCount("v1");
        countObj.start = 1;
        list.add(countObj);
        KeystrokeCount countObj2 = new KeystrokeCount("v1");
        countObj.start = 2;
        list.add(countObj2);

        KeystrokeCount maxobj = Collections.max(list, new KeystrokeCount.SortByLatestStart());

        Assert.assertTrue(maxobj.start == 2);
    }

    @Test
    public void test_single_collection_sort() {
        List<KeystrokeCount> list = new ArrayList<>();
        KeystrokeCount countObj = new KeystrokeCount("v1");
        countObj.start = 1;
        list.add(countObj);

        KeystrokeCount maxobj = Collections.max(list, new KeystrokeCount.SortByLatestStart());

        Assert.assertTrue(maxobj.start == 1);
    }
}
