package com.voiceeffect;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.voiceeffect.effects.AudioTransform;
import com.voiceeffect.effects.AutoTune;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ToggleButton;

public class VoiceProcess extends Activity {
	
	public static final int BUFFER_OVERLAYS = 2;
	private Map<String, AudioTransform> effects;
	private AudioRecord audioInput;
	private AudioTrack audioOutput;
	private int bufferSize;

	public static int BUFFER_SIZE = 1024;
	
	public class ApplyTransforms implements Runnable {
		
		private AudioRecord audioInput;
		private AudioTrack audioOutput;
		private Map<String, AudioTransform> effects;
		private boolean isDone;
		
		
		public ApplyTransforms(
				final Map<String, AudioTransform> effects
				, final AudioRecord audioInput
				, final AudioTrack audioOutput) {
			this.effects = effects;
			this.audioInput = audioInput;
			this.audioOutput = audioOutput;
			this.audioInput.startRecording();
			this.audioOutput.play();
		}
		
		public void run() {
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			byte[] buf = new byte[bufferSize*2];
			byte[] transformedBuf = new byte[bufferSize*2];
			
			while(!this.isDone) {

				audioInput.read(buf, 0, bufferSize*2);
				
				for(Map.Entry<String, AudioTransform> effect : effects.entrySet()) {
					for(int i = 0; i < BUFFER_OVERLAYS; i++) {
						System.arraycopy(
								effect.getValue().transform(
										ArrayUtils.subarray(
												buf
												, i*(bufferSize / BUFFER_OVERLAYS)
												, bufferSize))
								, 0, transformedBuf, i*(bufferSize / BUFFER_OVERLAYS)
								, bufferSize);
					}
				}
				
				audioOutput.write(transformedBuf, 0, bufferSize*2);
				System.arraycopy(buf, bufferSize, buf, 0, bufferSize);
				
			}
		}
		
		public synchronized void setDone(final boolean isDone) {
			this.isDone = isDone;
		}
	}
	
	private ApplyTransforms transform;
	private Thread transformThread;
	
	public VoiceProcess() {
		this.effects = new HashMap<String, AudioTransform>();
		this.audioInput = getAudioRecord();
		this.bufferSize = BUFFER_SIZE; 
		if(this.audioInput == null) {
			throw new RuntimeException("couldn't initialize an AudioRecord object.");
		}
		this.audioOutput = new AudioTrack(
				AudioManager.STREAM_VOICE_CALL
				, this.rate
				, this.channelConfig == AudioFormat.CHANNEL_IN_MONO ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO
				, this.audioFormat
				, this.bufferSize
				, AudioTrack.MODE_STREAM);
		this.audioOutput.setPlaybackRate(rate);
		this.transform = new ApplyTransforms(
				this.effects
				, this.audioInput
				, this.audioOutput);
		this.transformThread = new Thread(this.transform);
		this.transformThread.start();
	}
	
	private int rate;
	private short audioFormat;
	private short channelConfig;
	
	private AudioRecord getAudioRecord() {
		AudioRecord audioInput = null;
		for(int rate : new int[] {8000, 11025, 22050, 44100 }) {
			for(short audioFormat : new short[] {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
				for(short channelConfig: new short[] {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
					try {
						int bufferSize = BUFFER_SIZE;
						if(bufferSize < 0) {
							continue;
						} 
						AudioRecord candidateInput = new AudioRecord(
								MediaRecorder.AudioSource.MIC
								, rate
								, channelConfig
								, audioFormat
								, bufferSize);
						if(candidateInput.getState() == AudioRecord.STATE_INITIALIZED && rate > this.rate) {
							this.audioFormat = audioFormat;
							this.channelConfig = channelConfig;
							this.rate = rate;
							this.bufferSize = bufferSize;
							audioInput = candidateInput;
						} else {
							candidateInput = null;
						}
					} catch(final Exception e) {
						continue;
					}
				}
			}
		}
		return audioInput;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_process);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.voice_process, menu);
        return true;
    }
    
    public void toggleAutoTune(View toggleContext) {
    	boolean on = ((ToggleButton) toggleContext).isChecked();
    	if(on) {
    		addAudioEffect(
    				getString(R.string.autoTuneToggleLabel)
    				, new AutoTune(this.bufferSize));
    	} else {
    		removeAudioEffect(getString(R.string.autoTuneToggleLabel));
    	}
    }

	private synchronized void removeAudioEffect(final String effectLabel) {
		this.effects.remove(effectLabel);
	}

	private synchronized void addAudioEffect(
			final String effectLabel,
			final AutoTune autoTune) {
		this.effects.put(effectLabel, autoTune);
	}

}
