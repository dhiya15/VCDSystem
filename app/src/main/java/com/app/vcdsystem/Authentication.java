package com.app.vcdsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.studioidan.httpagent.HttpAgent;
import com.studioidan.httpagent.JsonArrayCallback;
import com.studioidan.httpagent.JsonCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import models.VoyageTrack;

public class Authentication extends AppCompatActivity {

    private EditText userUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        userUserName = findViewById(R.id.userUserName);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS}, 1000);
    }

    public void start(View view) {
        String user = userUserName.getText().toString();
        if(! user.isEmpty()){
            HttpAgent.get("https://t-gestion.herokuapp.com/t-gestion/api/v1/voyage/"+user)
                    .goJson(new JsonCallback() {
                        @Override
                        protected void onDone(boolean success, JSONObject jsonObject) {
                            try {
                                if(success && (jsonObject != null)) {

                                    VoyageTrack.code = jsonObject.getString("code");
                                    VoyageTrack.depart = jsonObject.getString("depart");
                                    VoyageTrack.arrive = jsonObject.getString("arrive");
                                    VoyageTrack.heur = jsonObject.getString("heur");
                                    VoyageTrack.jour = jsonObject.getString("jours");

                                    JSONObject obj = jsonObject.getJSONObject("bus");
                                    VoyageTrack.bus.matricule = obj.getString("matricule");
                                    VoyageTrack.bus.nbrPlace = obj.getInt("nbrPlace");

                                    obj = jsonObject.getJSONObject("chauffeur");
                                    VoyageTrack.chauffeur.numCarteID = obj.getString("numCarteID");
                                    VoyageTrack.chauffeur.nomComplet = obj.getString("nomComplet");
                                    VoyageTrack.chauffeur.telephone = obj.getString("telephone");

                                    obj = jsonObject.getJSONObject("receveur");
                                    VoyageTrack.receveur.numCarteID = obj.getString("numCarteID");
                                    VoyageTrack.receveur.nomComplet = obj.getString("nomComplet");
                                    VoyageTrack.receveur.telephone = obj.getString("telephone");

                                    Intent intent = new Intent(Authentication.this, NavigationActivity.class);
                                    startActivity(intent);

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
        }
    }




}