package com.example.lndtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import lndmobile.*;
import lnrpc.Rpc;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";

    long unlockstart = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void WriteConfig(View view) {
        String filename = getApplicationContext().getFilesDir().toString() + "/lnd.conf";

        try {
            new File(filename).getParentFile().mkdirs();

            PrintWriter out = new PrintWriter(filename);

            out.println(
                "[Application Options]\n" +
                "debuglevel=info\n" +
                "no-macaroons=1\n" +
                "maxbackoff=2s\n" +
                "nolisten=1\n" +
                "norest=1\n" +
                "sync-freelist=1\n" +
                "\n" +
                "[Routing]\n" +
                "routing.assumechanvalid=1\n" +
                "\n" +
                "[Bitcoin]\n" +
                "bitcoin.active=1\n" +
                "bitcoin.testnet=1\n" +
                "bitcoin.node=neutrino\n" +
                "\n" +
                "[Neutrino]\n" +
                //"neutrino.connect=btcd-testnet.lightning.computer\n" +
                "neutrino.feeurl=https://nodes.lightning.computer/fees/v1/btc-fee-estimates.json\n" +
                "\n" +
                "[autopilot]\n" +
                "autopilot.active=0\n" +
                "autopilot.private=1\n" +
                "autopilot.minconfs=1\n" +
                "autopilot.conftarget=3\n" +
                "autopilot.allocation=1.0\n" +
                "autopilot.heuristic=externalscore:0.95\n" +
                "autopilot.heuristic=preferential:0.05\n"
            );
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Config written");
    }

    public void StartLnd(View view) {
        Log.i(TAG, "startLnd");
        //TextView err = (TextView)findViewById(R.id.statusText);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String filename = getApplicationContext().getFilesDir().getPath();
                Log.i(TAG, filename);
                final long start = System.currentTimeMillis();
                Lndmobile.start(
                    "--lnddir=" + filename,
                    new lndmobile.Callback() {
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "lnd init error", e);
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            Log.i(TAG, "lnd init onResponse");
                            Log.i(TAG, "lnd init callback time: " + (System.currentTimeMillis() - start));
                        }
                    },
                    new lndmobile.Callback() {
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Wallet unlock error", e);
                        }

                        @Override
                        public void onResponse(byte[] bytes) {
                            Log.i(TAG, "wallet RPC ready callback onResponse");
                            Log.i(TAG, "wallet RPC ready callback time: " + (System.currentTimeMillis() - unlockstart));
                        }
                    }
                );
            }
        });
        t.start();
    }

    public void InitWallet(View view) {
        final long start = System.currentTimeMillis();
        lndmobile.Lndmobile.genSeed(
            lnrpc.Rpc.GenSeedRequest.getDefaultInstance().toByteArray(),
            new Callback() {
                @Override
                public void onError(Exception e) {

                }

                @Override
                public void onResponse(byte[] bytes) {
                    lnrpc.Rpc.GenSeedResponse seed;
                    try {
                        seed = lnrpc.Rpc.GenSeedResponse.parseFrom(bytes);
                    } catch (InvalidProtocolBufferException e) {
                        Log.e(TAG, "Error");
                        e.printStackTrace();
                        return;
                    }

                    Log.i(TAG, seed.getCipherSeedMnemonicList().toString());

                    lnrpc.Rpc.InitWalletRequest.Builder initWallet = lnrpc.Rpc.InitWalletRequest.newBuilder();
                    initWallet.addAllCipherSeedMnemonic(seed.getCipherSeedMnemonicList());
                    initWallet.setWalletPassword(ByteString.copyFromUtf8("test1234"));

                    lndmobile.Lndmobile.initWallet(
                        initWallet.build().toByteArray(),
                        new Callback() {
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "onError initWallet");
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(byte[] bytes) {
                                Log.i(TAG, "onResponse initWallet");
                                Log.i(TAG, "onResponse time: " + (System.currentTimeMillis() - start));
                            }
                        }
                    );
                }
            }
        );
    }

    public void UnlockWallet(View view) {
        final long start = System.currentTimeMillis();
        unlockstart = System.currentTimeMillis();
        lnrpc.Rpc.UnlockWalletRequest.Builder unlockWallet = lnrpc.Rpc.UnlockWalletRequest.newBuilder();
        unlockWallet.setWalletPassword(ByteString.copyFromUtf8("test1234"));

        lndmobile.Lndmobile.unlockWallet(
            unlockWallet.build().toByteArray(),
            new Callback() {
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "onError unlockWallet");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(byte[] bytes) {
                    Log.i(TAG, "onResponse unlockWallet");
                    Log.i(TAG, "onResponse unlockWallet time: "  + (System.currentTimeMillis() - start));
                }
            }
        );
    }

    public void StopDaemon(View view) {
        lnrpc.Rpc.StopRequest stopRequest = Rpc.StopRequest.newBuilder().build();

        lndmobile.Lndmobile.stopDaemon(
            stopRequest.toByteArray(),
            new Callback() {
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "onError stopDaemon");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(byte[] bytes) {
                    Log.i(TAG, "onResponse stopDaemon");
                    Log.i(TAG, "Daemon should be stopped");
                }
            }
        );
    }
}
