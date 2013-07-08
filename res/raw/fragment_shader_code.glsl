precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;
uniform float fAlpha;

void main() {
	vec4 color = texture2D(sTexture, vTextureCoord);
	gl_FragColor = vec4(color.xyz, color.w * fAlpha);
	//gl_FragColor = vec4(0.6, 0.7, 0.2, 1.0);
}
