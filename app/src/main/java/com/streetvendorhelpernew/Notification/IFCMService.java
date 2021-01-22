package com.streetvendorhelpernew.Notification;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAI_2cHrQ:APA91bEOt8hD7OHn7Fm_bEo-wKJwMhxZSnYKcW9z5rrcPXxqsNP5ESJ_4fH110FFI0vvaJAU2e5kFW75uvRY9WGcVNk6RjbsCmQu48JNvLWC7eBekLiyPgYN9tWp9GPUsumB1_4r4WC6"
    })

    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);
}
