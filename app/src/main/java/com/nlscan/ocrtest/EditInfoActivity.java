package com.nlscan.ocrtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nlscan.ocrtest.util.Constants;

import java.util.regex.Pattern;

public class EditInfoActivity extends BaseActivity {

    private EditText et_re_name,et_re_phone,et_re_address,
            et_se_name,et_se_phone,et_se_address,
            et_good_type,et_good_weight,et_good_code;

    private String reName,rePhone,reAddress,seName,sePhone,seAddress,goodType,goodWeight,goodCode;

    private Button btn_info_confirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);
        setTitle("寄件信息编辑");

        et_re_name = (EditText) findViewById(R.id.et_re_name);
        et_re_phone = (EditText) findViewById(R.id.et_re_phone);
        et_re_address = (EditText) findViewById(R.id.et_re_address);

        et_se_name = (EditText) findViewById(R.id.et_se_name);
        et_se_phone = (EditText) findViewById(R.id.et_se_phone);
        et_se_address = (EditText) findViewById(R.id.et_se_address);

        et_good_type = (EditText) findViewById(R.id.et_good_type);
        et_good_weight = (EditText) findViewById(R.id.et_good_weight);
        et_good_code = (EditText) findViewById(R.id.et_good_code);

        btn_info_confirm = (Button) findViewById(R.id.btn_info_confirm);



        reName = getIntent().getStringExtra(Constants.RE_NAME);
        rePhone = getIntent().getStringExtra(Constants.RE_PHONE);
        reAddress = getIntent().getStringExtra(Constants.RE_ADDRESS);

        seName = getIntent().getStringExtra(Constants.SE_NAME);
        seAddress = getIntent().getStringExtra(Constants.SE_ADDRESS);
        sePhone = getIntent().getStringExtra(Constants.SE_PHONE);

        goodCode = getIntent().getStringExtra(Constants.SE_CODE);
        goodType = getIntent().getStringExtra(Constants.SE_TYPE);
        goodWeight = String.valueOf(getIntent().getFloatExtra(Constants.SE_WEIGHT,1.0f)) ;


        et_re_name.setText(reName);
        et_re_phone.setText(rePhone);
        et_re_address.setText(reAddress);

        et_se_name.setText(seName);
        et_se_address.setText(seAddress);
        et_se_phone.setText(sePhone);

        et_good_code.setText(goodCode);
        et_good_type.setText(goodType);
        et_good_weight.setText(goodWeight);


        btn_info_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reName = et_re_name.getText().toString();
                reAddress = et_re_address.getText().toString();
                rePhone = et_re_phone.getText().toString();

                seName = et_se_name.getText().toString();
                seAddress = et_se_address.getText().toString();
                sePhone = et_se_phone.getText().toString();

                goodCode = et_good_code.getText().toString();
                goodType = et_good_type.getText().toString();
                goodWeight = et_good_weight.getText().toString();

                String parrten = "\\d*\\.?\\d*";



                boolean ifEmpty =  !"".equals(reName) && !"".equals(reAddress) && !"".equals(rePhone) &&
                        !"".equals(seName) && !"".equals(sePhone) && !"".equals(seAddress) &&
                        !"".equals(goodCode) && !"".equals(goodType) && !"".equals(goodWeight) ;

                if (ifEmpty){
                    Intent intent = new Intent(EditInfoActivity.this, VerifyActivity.class);

                    intent.putExtra(Constants.SE_NAME,seName);
                    intent.putExtra(Constants.SE_PHONE,sePhone);
                    intent.putExtra(Constants.SE_ADDRESS,seAddress);
                    intent.putExtra(Constants.RE_NAME,reName);
                    intent.putExtra(Constants.RE_PHONE,rePhone);
                    intent.putExtra(Constants.RE_ADDRESS,reAddress);
                    intent.putExtra(Constants.SE_TYPE,goodType);
                    intent.putExtra(Constants.SE_WEIGHT,goodWeight);
                    intent.putExtra(Constants.SE_CODE,goodCode);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    if (Pattern.matches(parrten,goodWeight)){
                        startActivity(intent);
                    }
                    else{
                        Toast.makeText(EditInfoActivity.this,"重量格式不对，请输入整数或小数!",Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(EditInfoActivity.this,"信息不能有空!",Toast.LENGTH_SHORT).show();
                }

            }
        });



    }
}
