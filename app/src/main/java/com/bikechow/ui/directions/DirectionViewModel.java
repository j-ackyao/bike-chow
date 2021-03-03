package com.bikechow.ui.directions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DirectionViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public DirectionViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is directions fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}