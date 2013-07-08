uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;

void main() {
	gl_Position = uMVPMatrix * vPosition;
	vTextureCoord = aTextureCoord;
}
