package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.plugins.dataclasses.Response;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class ServerReviewsAPI {

    public static final String API_URL = "https://manti.vendicated.dev";
    public static final int AdFlag = 0b00000001;
    public static final int Warning = 0b00000010;

    public static Response simpleRequest(String endpoint, String method, JSONObject body) {
        try {
            Http.Response response;

            if (body == null)
                response = new Http.Request(API_URL + endpoint, method).execute();
            else
                response = new Http.Request(API_URL + endpoint, method).setFollowRedirects(false).executeWithBody(body.toString());


            var json = response.json(Response.class);
            ServerReviews.logger.info(json.toString());
            return json;

        } catch (IOException e) {
            ServerReviews.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    public static List<Review> getReviews(long userid) {
        int flags = 0;
        if (ServerReviews.staticSettings.getBool("disableAds",false))
            flags |= AdFlag;
        if (ServerReviews.staticSettings.getBool("disableWarnings",false))
            flags |= Warning;
        var response = simpleRequest("/api/reviewdb/users/" + userid +"/reviews?flags=" + flags,"GET", null);
        if (!response.isSuccessful()) {
            return null;
        }
        return response.getReviews() ;
    }

    public static Response reportReview(String token,int reviewID) {
        JSONObject json = new JSONObject();
        try {
            json.put("token",token);
            json.put("reviewid",reviewID);

            return simpleRequest("/api/reviewdb/reports","POST",json);
        } catch (JSONException e) {
            ServerReviews.logger.error(e);
            return null;
        }
    }

    public static Response deleteReview(String token,int reviewid) {
        try{
            var json = new JSONObject();
            json.put("token",token);
            json.put("reviewid",reviewid);

            return simpleRequest("/api/reviewdb/users/0/reviews","DELETE",json);

        } catch (JSONException e) {
            ServerReviews.logger.error(e);
            return new Response(false,false,"An Error Occured");
        }
    }

    public static Response addReview(String comment, Long userid, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("comment", comment);
            json.put("token", token);
            json.put("reviewtype",1);

            return simpleRequest("/api/reviewdb/users/" + userid + "/reviews","PUT",json);

        } catch (JSONException e) {
            e.printStackTrace();
            new Logger("guh").error(e);
            return new Response(false, false, "An Error Occured");
        }
    }
}
