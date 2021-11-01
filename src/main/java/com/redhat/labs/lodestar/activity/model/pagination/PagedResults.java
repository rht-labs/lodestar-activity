package com.redhat.labs.lodestar.activity.model.pagination;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple class holding a results from a number of paged queries to gitlab
 * @author mcanoy
 *
 * @param <T>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResults<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(PagedResults.class);
    
    @Setter(value = AccessLevel.NONE) private int pageSize;
    @Builder.Default @Setter(value = AccessLevel.NONE) private int number = 1;
    @Builder.Default @Setter(value = AccessLevel.NONE) private int total = 10000;
    @Builder.Default @Setter(value = AccessLevel.NONE) private List<T> results = new ArrayList<>();
    
    public PagedResults(int pageSize) {
        this();
        this.pageSize = pageSize;
    }

    public boolean hasMore() {
        return total >= number;
    }

    public void end() {
        total = 0; // 0 = No results
    }
    
    public void update(Response response, GenericType<List<T>> type) {
        LOGGER.trace("page result update");
        
        List<T> responseSet = response.readEntity(type);
        
        //There weren't enough  results to fetch another page
        if(responseSet.size() < pageSize) {
            total = 1;
        }
        
        results.addAll(responseSet);
        number++;
    }
    
    public int size() {
        return results.size();
    }
  
}
