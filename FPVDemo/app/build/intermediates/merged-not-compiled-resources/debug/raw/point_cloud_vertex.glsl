precision mediump float;

attribute vec4 a_Position;
uniform mat4 u_MVP;
uniform float u_PointThickness;
uniform vec3 vCurrentPos;
uniform float vColorMask;
// color
attribute vec4 a_Color;
varying vec4 v_Color;
varying float v_Intensity;
// height
varying float v_Height;
varying float v_Distance;

void main() {
    gl_Position = u_MVP * a_Position;
    gl_PointSize = u_PointThickness;
    v_Color = a_Color;
    v_Intensity = v_Color.a * 255.0;
    v_Color.a = 1.0;
    v_Height = a_Position.z;

    if(vColorMask==2.0) {
        //空间距离
        v_Distance = sqrt(pow(vCurrentPos.x-a_Position.x, 2.0)
            + pow(vCurrentPos.y-a_Position.y, 2.0)
            + pow(vCurrentPos.z-a_Position.z, 2.0));
        v_Distance = abs(v_Distance);
    } else if(vColorMask==3.0) {
        //水平距离
        v_Distance = sqrt(pow(vCurrentPos.x-a_Position.x, 2.0)
        + pow(vCurrentPos.y-a_Position.y, 2.0));
        v_Distance = abs(v_Distance);
        v_Height = abs(vCurrentPos.z - a_Position.z);
    }
}