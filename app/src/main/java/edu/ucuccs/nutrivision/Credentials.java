/**
 * Author: UCU Knight Coders on 9/13/2016.
 * Website: http://facebook.com/teamucuccs
 * E-mail: teamucuccs@gmail.com
 */

package edu.ucuccs.nutrivision;

class Credentials {

    public String  nutrionixURL;

    public interface CLARIFAI {
        String CLIENT_ID = "F5fbBfo8MZtjMeeNWn_Fc9pC-u97usHofgiNz5tp";
        String CLIENT_SECRET = "IhQv11zTu7slc_SQ9YTVdrKz40ufVPIzm2noKnkp";
    }
    public interface NUTRIONIX {
        String APP_ID = "f8d8df72";
        String API_KEY = "d9aea37b804ffef0a9cdc1378638706c";
    }

    public String returnURL(String keyword){
        nutrionixURL = "https://api.nutritionix.com/v1_1/search/" + keyword + "?fields=item_name%2Citem_id%2Cbrand_name%2Cnf_calories%2Cnf_total_fat&appId=" + NUTRIONIX.APP_ID + "&appKey=" + NUTRIONIX.API_KEY;
        return nutrionixURL;
    }
}