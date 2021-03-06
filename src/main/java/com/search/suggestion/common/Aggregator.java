package com.search.suggestion.common;

import static com.search.suggestion.common.Precondition.checkPointer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.search.suggestion.data.Bucket;
import com.search.suggestion.data.ScoredObject;
import com.search.suggestion.data.SearchPayload;
import com.search.suggestion.data.Suggestable;

/**
 * Aggregator to collect, merge and transform {@link ScoredObject} elements.
 */
@SuppressWarnings("unchecked")
public class Aggregator<T extends Suggestable>
{
    private final Map<T, Double> scores;
    private final Comparator<ScoredObject<T>> comparator;

    /**
     * Constructs a new {@link Aggregator}.
     */
    public Aggregator()
    {
        this(null);
    }

    /**
     * Constructs a new {@link Aggregator}.
     */
    public Aggregator(@Nullable Comparator<ScoredObject<T>> comparator)
    {
        this.scores = new HashMap<>();
        this.comparator = comparator;
    }

    /**
     * Adds a single element, if not present already.
     *
     * @throws NullPointerException if {@code element} is null;
     */
    public boolean add(ScoredObject<T> element)
    {
        return addAll(Arrays.asList(element));
    }

    /**
     * Adds a collection of elements, if not present already.
     *
     * @throws NullPointerException if {@code elements} is null or contains a null element;
     */
    public boolean addAll(Collection<ScoredObject<T>> elements)
    {
        checkPointer(elements != null);
        boolean result = false;
        for (ScoredObject<T> element : elements)
        {
            checkPointer(element != null);
            Double score = scores.get(element.getObject());

            if (score == null || element.getScore()==0)
            {
                scores.put(element.getObject(), element.getScore());
                result = true;
            }
            else if (element.getScore() != 0)
            {

                scores.put(element.getObject(), score + element.getScore());
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if no elements exist.
     */
    public boolean isEmpty()
    {
        return scores.isEmpty();
    }

    /**
     * Retain the elements in common, compared according to the objects scored.
     *
     * @throws NullPointerException if {@code element} is null;
     */
    public boolean retain(ScoredObject<T> element)
    {
        return retainAll(Arrays.asList(element));
    }

    /**
     * Retains the elements in common, compared according to the objects scored.
     *
     * @throws NullPointerException if {@code elements} is null or contains a null element;
     */
    public boolean retainAll(Collection<ScoredObject<T>> elements)
    {
        checkPointer(elements != null);
        // Intersect
        Collection<T> set = new HashSet<>();
        for (ScoredObject<T> element : elements)
        {
            checkPointer(element != null);
            set.add(element.getObject());
        }
        boolean result = scores.keySet().retainAll(set);
        // Combine scores
        for (ScoredObject<T> element : elements)
        {
            if (element.getScore() == 0)
            {
                continue;
            }
            Double score = scores.get(element.getObject());
            if (score != null)
            {
                scores.put(element.getObject(), score + element.getScore());
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns the number of elements.
     */
    public int size()
    {
        return scores.size();
    }

    /**
     * Returns a {@link List} of all objects scored, sorted according to the default comparator.
     */
    public List<T> values()
    {
        List<ScoredObject<T>> list = new ArrayList<>();
        for (Entry<T, Double> entry : scores.entrySet())
        {
            list.add(new ScoredObject<>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(list, comparator);
        List<T> result = new ArrayList<>();
        for (ScoredObject<T> element : list)
        {
            result.add(element.getObject());
        }
        return result;
    }
    /**
     * Returns a {@link List} of all objects scored, sorted according to the default comparator.
     */
	public TreeMap<Double, List<Suggestable>> values(SearchPayload json, boolean bool)
    {
        String bucketKey = json.getFirstBucket();

    	Double value;
		TreeMap<Double, List<Suggestable>> tmap = new TreeMap<Double, List<Suggestable>>(Collections.reverseOrder());
        
        for (Entry<T, Double> entry : scores.entrySet())
        {
			T oldsr = entry.getKey();
			Suggestable sr = oldsr.copy();
        	
        	value = entry.getValue();
        	value = (double)Math.round(value * 100) / 100;

            UpdateMap(tmap,sr,value);
        }
        return tmap;
    }
    /**
     * Returns a {@link List} of all objects scored, sorted according to the default comparator.
     */
    public List<T> values(SearchPayload json)
    {
        int maxBucketSize = 3;
        ArrayList<ArrayList<T>> bucketList = new ArrayList<>();

		Map<String, Bucket> buckets = json.getBucket();

		int bucketSize = buckets.size();
        String bucketName[];
        int bucketValue[];
        int bucketWeight[];
        int size=1;
        if(bucketSize == 1) {
            size=2;
        }
        if(bucketSize == 2) {
            size=4;
        }
        if(bucketSize == 3) {
            size=8;
        }
        bucketName = new String[maxBucketSize];
        bucketValue = new int[maxBucketSize];
        bucketWeight = new int[maxBucketSize];
        for(int i =0; i<maxBucketSize;i++) {
            bucketName[i]   = "";
            bucketValue[i]  = 0;
            bucketWeight[i] = 0;
        }
        for(int i=0; i<scores.size();i++) {

            ArrayList<T> arrayListWithBucket = new ArrayList<>();
            bucketList.add(arrayListWithBucket);
        }
        int start = 0;
		if (buckets.size() > 0) {
			for (Entry<String, Bucket> entry : buckets.entrySet()) {
                bucketName[start]   = entry.getKey();
                bucketValue[start]  = entry.getValue().value;
                bucketWeight[start] = entry.getValue().weight;
                start++;
            }
        }

        Double value;
		TreeMap<Double, List<Suggestable>> tmap = new TreeMap<Double, List<Suggestable>>(Collections.reverseOrder());

        for (Entry<T, Double> entry : scores.entrySet()) {
			T oldsr = entry.getKey();
			Suggestable sr = oldsr.copy();

            value = entry.getValue();
            value = (double) Math.round(value * 100) / 100;

            UpdateMap(tmap, sr, value);
        }
        Set set = tmap.entrySet();
        Iterator iterator = set.iterator();
        List<T> result = new ArrayList<>();
        int st = 0;
        while (iterator.hasNext()) {
            Entry mentry = (Entry) iterator.next();
			ArrayList<Suggestable> al = (ArrayList<Suggestable>) mentry.getValue();

			Map<Integer, TreeMap<Integer, List<Suggestable>>> tempBucket = new HashMap<>();
            for(int i=0; i<size;i++) {
				TreeMap<Integer, List<Suggestable>> bucketZero = new TreeMap<Integer, List<Suggestable>>(Collections.reverseOrder());
                tempBucket.put(i, bucketZero);
            }

            for (int i = 0; i < al.size(); i++) {

				Suggestable sr = al.get(i);

                boolean first = same(sr, json, bucketName[0]);
                boolean second = same(sr, json, bucketName[1]);
                boolean third = same(sr, json, bucketName[2]);

                if(bucketSize == 0) {
                    UpdateMap(tempBucket.get(0), sr, sr.getCount());
                }
                if (bucketSize == 1) {
                    if (first) {
                        UpdateMap(tempBucket.get(0), sr, sr.getCount());
                    }
                    else {
                        UpdateMap(tempBucket.get(1), sr, sr.getCount());
                    }
                }
                if (bucketSize == 2) {
                    if (first && second) {
                        UpdateMap(tempBucket.get(0), sr, sr.getCount());
                    }
                    else if ((first && !second) || (!first && second)) {
                        int wgt1 = bucketWeight[0];
                        int wgt2 = bucketWeight[1];
                        if(wgt1 > wgt2 && first) {
                            UpdateMap(tempBucket.get(1), sr, sr.getCount());
                        }
                        else if(wgt2 > wgt1 && second) {
                            UpdateMap(tempBucket.get(1), sr, sr.getCount());
                        }
                        else {
                            UpdateMap(tempBucket.get(2), sr, sr.getCount());
                        }
                    }
                    else {
                        UpdateMap(tempBucket.get(3), sr, sr.getCount());
                    }
                }
                if (bucketSize == 3) {
                    List<Integer> weights = new ArrayList<Integer>();

                    weights.add(0);
                    weights.add(bucketWeight[2]);
                    weights.add(bucketWeight[1]);
                    weights.add(bucketWeight[2]+bucketWeight[1]);
                    weights.add(bucketWeight[0]);
                    weights.add(bucketWeight[0]+bucketWeight[2]);
                    weights.add(bucketWeight[0]+bucketWeight[1]);
                    weights.add(bucketWeight[0]+bucketWeight[1] + bucketWeight[2]);
                    Collections.reverse(weights);

                    int totalWeight = 0;
                    if(first)
                        totalWeight+=bucketWeight[0];
                    if(second)
                        totalWeight+=bucketWeight[1];
                    if(third)
                        totalWeight+=bucketWeight[2];

                    int find = 0;
                    for(int weight : weights) {
                        if(weight == totalWeight)
                            break;
                        find++;
                    }

                        UpdateMap(tempBucket.get(find), sr, sr.getCount());
                }
            }
            for(int i = 0; i < size; i++) {
                if(tempBucket.get(i).size()>0)
                    UpdateResults(bucketList.get(st), tempBucket.get(i));
            }
            st++;


        }

        for (int i = 0; i < bucketList.size(); i++) {
            UpdateResultsList(result, bucketList.get(i));
        }

        return result;
    }

    private void UpdateResultsList(List<T> result, List<T> t) {
        for(T sp : t) {
            result.add(sp);
        }
    }

	private boolean same(Suggestable sr, SearchPayload json, String key) {

		if (!key.equals("") && sr.getFilter().get(key) != null && json.getBucket(key) != null) {
			if (Integer.parseInt(sr.getFilter().get(key).toString()) == json.getBucket(key).value) {
                return true;
            }
        }
        return false;
    }

	public void UpdateResults(List<T> result, Map<Integer, List<Suggestable>> tmap) {
    	Set set = tmap.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
        	Entry mentry = (Entry)iterator.next();
			ArrayList<Suggestable> al = (ArrayList<Suggestable>) mentry.getValue();
        	for(int i=0;i<al.size();i++) {
        		result.add((T)al.get(i));
        	}
        }
    }

	public <T> void UpdateMap(TreeMap<T, List<Suggestable>> tmap, Suggestable sr, T key) {

    	if(tmap.containsKey(key)) {
			List<Suggestable> templist = tmap.get(key);
		    boolean norecord = true;

			for (Suggestable srt : templist) {
				Suggestable srs = srt;

                if(sr.getSearch().equals(srs.getSearch()) || sr.getRealText().equals(srs.getRealText())) {
		    		norecord = false;
		    		srs.setCount(srs.getCount()+sr.getCount());
		    		break;
		    	}
		    }
		    if(norecord) {
	       		templist.add(sr);
	       		tmap.remove(key);
	       		tmap.put(key, templist);
		    }
	   }
	   else
	   {
			List<Suggestable> templist = new ArrayList<Suggestable>();
		   templist.add(sr);
		   tmap.put(key, templist);
	   }
    }
}
