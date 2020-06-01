package com.tabourless.queue.data;

import androidx.paging.DataSource;

import com.tabourless.queue.interfaces.FirebaseOnCompleteCallback;
import com.tabourless.queue.models.Customer;
import com.tabourless.queue.models.Message;

import java.util.List;

public class CustomersDataFactory extends DataSource.Factory<String, Customer>{

    private String mPlaceKey, mQueueKey;
    private CustomersDataSource mCustomersDataSource;

    // receive chatKey on the constructor
    public CustomersDataFactory(String placeKey, String queueKey) {
        this.mPlaceKey = placeKey;
        this.mQueueKey = queueKey;
        mCustomersDataSource = new CustomersDataSource(mPlaceKey, mQueueKey);
    }

    public void setScrollDirection(int scrollDirection, int lastVisibleItem) {
        mCustomersDataSource.setScrollDirection(scrollDirection, lastVisibleItem);
    }

    public void removeCustomer(Customer customer) {
        mCustomersDataSource.removeCustomer(customer);
    }

    public void removeListeners() {
        mCustomersDataSource.removeListeners();
    }

    @Override
    public DataSource<String, Customer> create() {
        return mCustomersDataSource;
    }

}