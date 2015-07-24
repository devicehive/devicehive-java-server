package com.devicehive.service.helpers;

import com.devicehive.model.HazelcastEntity;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

public class HazelcastEntityComparator implements Comparator<Map.Entry>, Serializable {
    private static final long serialVersionUID = 5413354955792888308L;

    @Override
    public int compare(Map.Entry o1, Map.Entry o2) {
        final Date o1Time = ((HazelcastEntity) o1.getValue()).getTimestamp();
        final Date o2Time = ((HazelcastEntity) o2.getValue()).getTimestamp();

        return o2Time.compareTo(o1Time);
    }
}
