uniform float vColorMask;
varying vec4 v_Color;
varying float v_Intensity;
varying float v_Height;
varying float v_Distance;

void main()
{
    if(vColorMask==0.0) {
        gl_FragColor = dji_colormap_intensity(v_Intensity);
    } else if(vColorMask==1.0) {
        gl_FragColor = dji_colormap_hybird3(v_Intensity, v_Height);
    } else if (vColorMask==2.0) {
        gl_FragColor = dji_colormap_distance(v_Distance);
    } else if (vColorMask==3.0) {
        gl_FragColor = dji_colormap_hybird(v_Intensity, v_Height, v_Distance);
    } else if (vColorMask==100.0) {
        gl_FragColor = vec4(0.5,0.5,0.5,0.8);
    } else if (vColorMask==101.0) {
        gl_FragColor = vec4(0.8,0,0,1.0);
    } else if (vColorMask==102.0) {
        gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    } else {
        if(v_Color.r ==0.0 && v_Color.g ==0.0 && v_Color.b == 0.0) {
            discard;
        }
        gl_FragColor = v_Color;
    }
}