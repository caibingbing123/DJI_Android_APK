/**
    hsv.x is the hue.色相
    hsv.y is the saturation.饱和度
    hsv.z is the value.亮度
*/
vec4 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return vec4(c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y), 1.0);
}

vec4 dji_colormap_intensity(float x) {
    float max_intensity = 170.0;
    float max_hsv = 240.0;
    x = x > max_intensity ? max_intensity : x;
    // 使用HSV色环0（红色）~240（蓝色）的一段，反射强度0显示为蓝色，反射强度170显示为红色
    float hue = max_hsv - (x / max_intensity * max_hsv) +1.0;
    return hsv2rgb(vec3(hue/360.0, 1.0, 1.0));
}

float height_to_hue(float height) {
    float max_height_above = 500.0;
    float max_height_below = -120.0;
    float max_hue = 240.0;
    float max_hue_sep = 50.0;

    height = height * -1.0;
    float tmpHeight = height;
    float hue;
    float tmpHue;

    if (height < 0.0) {
        if (height < max_height_below) {
            height = max_height_below;
        }
        //值域做了拉伸，高度在（-120，0）米之间的值映射成（2，50），所以小于-120米的高度截断
        tmpHue = 10.0 * log(1.0 / (1.0 - height)) + max_hue_sep;
        // 使用HSV色环240（蓝色）~300（紫色）的一段，高度-0米显示为蓝色，高度-120米显示为紫色
        hue = ((max_hue_sep - tmpHue)/ 48.0 * 60.0) + max_hue;
    } else {
        if (height > max_height_above) {
            height = max_height_above;
        }
        //值域做了拉伸，高度在（0，500）米
        //vec4 dji_colormap_height(float 之间的值映射成（50，175），所以大于500米的高度截断
        tmpHue = 20.0 * log(height + 1.0) + max_hue_sep;
        // 使用HSV色环0（红色）~240（蓝色）的一段，高度0米显示为蓝色，高度500米显示为红色
        hue = max_hue - ((tmpHue - max_hue_sep) / 125.0 * max_hue);
    }
    return hue;
}

vec4 dji_colormap_height(float height) {
    float hue = height_to_hue(height);
    return hsv2rgb(vec3(hue/360.0, 1.0, 1.0));
}

vec4 dji_colormap_distance(float distance) {
    float max_distance = 100.0;
    if (distance > max_distance) {
        distance = max_distance;
    }
    return colormap_rainbow(1.0-distance/max_distance);
}

/**
    四参数方程
    x  0, 10, 50, 100
    y  0, 48, 200, 240
*/
float dji_fit_distance(float X) {
    float A = 265.202698848855;
    float B = -1.63438778962701;
    float C = 25.1850705710956;
    float D = -5.27719360726574E-05;

    float Y = (A - D) / (1.0 + pow(X/C, B)) + D;
    return Y;
}

/**
    四参数方程
    x  0, 10, 50, 200
    x  0, 1, 5, 10
    0,10,30,50
    y  100, 80, 50, 0
    100, 60, 10,5
*/
float dji_fit_height(float X) {
//    float A = 100.104718846157;
//    float B = 0.623887416578139;
//    float C = 952.651863447957;
//    float D = -265.088242747097;
//    float A = 105.09848465988;
//    float B = 0.412518732175273;
//    float C = 3.48579122274903E+15;
//    float D = -81529609.0141228;
//    float A = 100.000494862489;
//    float B = 2.15493919661304;
//    float C = 65.9351482053583;
//    float D = -40.7577549465483;
    float A = 100.000000037297;
    float B = 2.66362233284868;
    float C = 11.411295658947;
    float D = 3.14369142743234;
    float Y = (A - D) / (1.0 + pow(X/C, B)) + D;
    return Y;
}

/**
    四参数方程 未使用
    x  0, 10, 50, 500
    y  0, 48, 200, 240
*/
float dji_fit_height_above(float X) {
    float A = 240.684851871223;
    float B = -1.85301733540879;
    float C = 21.1710486883974;
    float D = -8.38661982261898E-06;

    float Y = (A - D) / (1.0 + pow(X/C, B)) + D;
    return Y;
}


/**
    四参数方程 未使用
    x  0, 10, 50, 120
    y  240, 285, 295, 300
*/
float dji_fit_height_below(float X) {
    float A = 307.378138884403;
    float B = -0.689919042954066;
    float C = 5.71450642181915;
    float D = 239.721904366028;

    float Y = (A - D) / (1.0 + pow(X/C, B)) + D;
    return Y;
}

/**
    四参数方程
    x  0, 10, 50, 150
    y  20, 40, 80, 100
    y  40, 60, 90, 100
    10, 50, 90, 100
    10, 30, 70, 100
*/
float dji_fit_intensity(float X) {
    /*float A = 103.836357601223;
    float B = -1.27484240753072;
    float C = 12.6237924962258;
    float D = 9.99865795769208;*/
    float A = 130.006822404437;
    float B = -0.99987221389472;
    float C = 50.0016801445037;
    float D = 9.99519367281046;

    float Y = (A - D) / (1.0 + pow(X/C, B)) + D;
    return Y;
}

float dji_fit_height2(float height) {
    float max_height_above = 500.0;
    float max_height_below = -120.0;
    float max_hue = 240.0;
    float max_hue_sep = 50.0;

    height = height * -1.0;
    float tmpHeight = height;
    float hue;
    float tmpHue;

    if (height < 0.0) {
        if (height < max_height_below) {
            height = max_height_below;
        }
        //值域做了拉伸，高度在（-120，0）米之间的值映射成（2，50），所以小于-120米的高度截断
        tmpHue = 10.0 * log(1.0 / (1.0 - height)) + max_hue_sep;
        // 使用HSV色环240（蓝色）~300（紫色）的一段，高度-0米显示为蓝色，高度-120米显示为紫色
        hue = ((max_hue_sep - tmpHue)/ 48.0 * 60.0) + max_hue;
    } else {
        if (height > max_height_above) {
            height = max_height_above;
        }
        //值域做了拉伸，高度在（0，500）米
        //vec4 dji_colormap_height(float 之间的值映射成（50，175），所以大于500米的高度截断
        tmpHue = 20.0 * log(height + 1.0) + max_hue_sep;
        // 使用HSV色环0（红色）~240（蓝色）的一段，高度0米显示为蓝色，高度500米显示为红色
        hue = max_hue - ((tmpHue - max_hue_sep) / 125.0 * max_hue);
    }
    return hue;
}

float dji_fit_height3(float max_height, float min_height, float height) {
    float hue = (height - min_height) * (1.0 - 300.0 / (max_height - min_height));
    return hue;
}

vec4 dji_colormap_hybird2(float intensity, float height) {
    float max_intensity = 150.0;
    if (intensity > max_intensity) {
        intensity = max_intensity;
    }

    float hue = dji_fit_height3(50.0, -10.0, height);
    float value = dji_fit_intensity(intensity);
    return hsv2rgb(vec3(hue/360.0, 1.0, value/100.0));
}

vec4 dji_colormap_hybird3(float intensity, float height) {
    float max_intensity = 150.0;
    if (intensity > max_intensity) {
        intensity = max_intensity;
    }

    float hue = dji_fit_height2(height);
    float value = dji_fit_intensity(intensity);
    return hsv2rgb(vec3(hue/360.0, 1.0, value/100.0));
}

vec4 dji_colormap_hybird(float intensity, float height, float distance) {
    float max_intensity = 150.0;
    if (intensity > max_intensity) {
        intensity = max_intensity;
    }

    float max_height = 50.0;
    if (height > max_height) {
        height = max_height;
    }

    float max_distance = 100.0;
    if (distance > max_distance) {
        distance = max_distance;
    }

    float hue = dji_fit_distance(distance);
    float saturation = dji_fit_intensity(intensity);
    float value = dji_fit_height(height);

    return hsv2rgb(vec3(hue/360.0, 1.0, saturation/100.0));
}