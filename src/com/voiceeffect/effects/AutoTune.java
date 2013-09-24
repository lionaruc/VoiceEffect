package com.voiceeffect.effects;

import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class AutoTune implements AudioTransform {

	public AutoTune(int sampleLength) {}
	
	public byte[] transform(byte[] audioBuffer) {
		
		double[][] sample = new double[2][audioBuffer.length];
		for(int i = 0; i < audioBuffer.length; i++) {
			sample[0][i] = audioBuffer[i];
			sample[1][i] = 0;
		}
		
		FastFourierTransformer.transformInPlace(sample, DftNormalization.STANDARD, TransformType.FORWARD);
		for(int i = 0; i < sample[1].length; i++) {
			sample[0][i] += Math.sin((double) i);
			sample[1][i] += Math.cos((double) i);
		}
		FastFourierTransformer.transformInPlace(sample, DftNormalization.STANDARD, TransformType.INVERSE);
		
		for(int i = 0; i < audioBuffer.length; i++) {
			audioBuffer[i] = (byte) sample[0][i];
		}
		
		return audioBuffer;
		
	}
	
}
