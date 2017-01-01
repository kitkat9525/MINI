/*
 *  Circuit
 *  SSD1306_ADAFRUIT_128X64 OLED
 *  D0    CLK,SCK
 *  D1    MOSI
 *  RST   RESET
 *  DC    A0
 *  CS    CS
 *  VCC   +5V   
 *  GND   GND
 *  
 *  HM-10 Bluetooth Low Energy Module 
 *  VDD   +3.3V
 *  GND   GND
 *  P00   PWRC
 *  P01   Rx   
 *  P03   Tx
 */

#include <SoftwareSerial.h> // UART
#include <SPI.h>
#include <TimeLib.h>
#include <string.h>
#include <math.h>

#include "U8glib.h" // Organic Liquid Crystal Display Library
#include "bitmap.h"

#define BLE_PWRC 7 // 8  // P00 8
#define BLE_Rx 5 // 6  // P03 7
#define BLE_Tx 6 // 7  // P02 6

// Standart Arduino UNO 
#define D0 10
#define D1  9
#define RST 13
#define A0 11
#define CS 12

/*
    Use Custom PCB Board
    #define D0 9
    #define D1  8
    #define RST 12
    #define A0 10
    #define CS 11
*/

#define MODE_DEFAULT 0
#define MODE_INIT 1

#define MOTOR 4
#define SW 3

SoftwareSerial BluetoothLESerial(BLE_Rx, BLE_Tx); // Rx, Tx
// U8glib Constructor
// U8GLIB_SSD1306_128X64 u8g(D0, D1, CS, A0); // SW SPI Com: SCK = 10, MOSI = 9, CS = 12, A0(DC) = 11 
U8GLIB_SSD1306_ADAFRUIT_128X64 u8g(D0, D1, CS, A0); // SW SPI Com: SCK = 10, MOSI = 9, CS = 12, A0 = 11 
// U8GLIB_SSD1306_ADAFRUIT_128X64 u8g(CS, A0);    // HW SPI Com: CS = 10, A0 = 9 (Hardware Pins are  SCK = 13 and MOSI = 11)

int mode = 0;
int sleep = 0;

byte xPos = 64;
byte yPos = 32;
byte rad = 28;

void ArduinoInit(void);
void BluetoothLEInit(void);
void OLEDInit(void);

void BluetoothLE(void);

int callObserver(void);
int sleepObserver(void);

void syncTime(char* data);
void motor(void);

void drawString(int x, int y, char* str);
void drawImage(int x, int y, int cnt, int h, uint8_t* bitmap);

void drawSMSMessage(char* msg);
void drawIncomingCall(char* msg);
void drawAnalogClock(void);
void showTimePin(int center_x, int center_y, double pl1, double pl2, double pl3);

void setup() {
    // put your setup code here, to run once:
    ArduinoInit();
    BluetoothLEInit();
    OLEDInit();
    delay(2500);
}

void loop() {
    // put your main code here, to run repeatedly:
    sleepObserver();
    BluetoothLE();
    /*
     * Picture Loop
     * U8glib에는 "Picture Loop"라는 특별한 프로그래밍 구조
     * 그래픽 장치의 전체 그림은 그림 루프를 반복하여 그려집니다.
     * https://github.com/olikraus/u8glib/wiki/tpictureloop
     */
    if(mode == MODE_INIT){
        drawImage((128-64)/2, 0, 8, 64, Bluetooth);        
    } else {
        u8g.firstPage();  
        do { 
             drawAnalogClock();
        } while(u8g.nextPage()); 
    } 
}

void ArduinoInit(void){
    pinMode(MOTOR, OUTPUT);
    pinMode(SW, INPUT);
    
    digitalWrite(MOTOR, HIGH);
    mode = MODE_INIT;
}

void BluetoothLEInit(void){
    BluetoothLESerial.begin(115200); // Set 115200 Baud Rate
    Serial.begin(115200); // Set 115200 Baud Rate

    // BLE Start Message
    Serial.println("ATcommand");  // ATcommand Start
    delay(50);
}

void OLEDInit(void) {
    // flip screen, if required
    // u8g.setRot180();
    
    // set SPI backup if required
    u8g.setHardwareBackup(u8g_backup_avr_spi);
    
    // assign default color value
    if ( u8g.getMode() == U8G_MODE_R3G3B2 )
        u8g.setColorIndex(255);     // white
    
    else if ( u8g.getMode() == U8G_MODE_GRAY2BIT )
        u8g.setColorIndex(3);         // max intensity
    
    else if ( u8g.getMode() == U8G_MODE_BW )
        u8g.setColorIndex(1);         // pixel on
    
    else if ( u8g.getMode() == U8G_MODE_HICOLOR )
        u8g.setHiColorByRGB(255,255,255);
}

void BluetoothLE(void){
    if (BluetoothLESerial.available()) { // 시리얼 통신으로 BluetoothLESerial로 온 데이터가 있을 경우.
        char buf[50] = {0};
        byte len = BluetoothLESerial.readBytes(buf, 50);
        char data[len] = {0};
    
        for(int i=0; i<len+1; i++)
            data[i] = buf[i];
        
        Serial.print(data);
        
        if(sleep) {
            sleep = 0;
            u8g.sleepOff();
        }
        
        if(mode == MODE_INIT){
            syncTime(data);
            mode = MODE_DEFAULT;
        } else if(mode == MODE_DEFAULT){
            if(data[0] == '#') {
                drawIncomingCall(strtok(data, "#"));
            } else if(data[0] == '&') {
                drawString(0, 0, "Call End");
                delay(500);
            } else{
                drawSMSMessage(data);
            }
        }
    } else if (Serial.available()) { // 시리얼 통신으로 Serial로 온 데이터가 있을 경우.
        // BluetoothLESerial.write(Serial.read());
        char buf[50] = {0};
        byte len = Serial.readBytes(buf, 50);
        char data[len] = {0};
    
        for(int i=0; i<len+1; i++)
            data[i] = buf[i];
    }
}

int callObserver(void){
    if (BluetoothLESerial.available()) { // 시리얼 통신으로 BluetoothLESerial로 온 데이터가 있을 경우.
        char buf[50] = {0};
        byte len = BluetoothLESerial.readBytes(buf, 50);
        char data[len] = {0};
    
        for(int i=0; i<len+1; i++)
            data[i] = buf[i];
            
       if(data[0] == '&')
          return 0;
    } else { 
        return 1;
    }     
}

int sleepObserver(void){
    if(sleep)
        u8g.sleepOn();
    else 
        u8g.sleepOff();
        
    if(!digitalRead(SW)){
        if(!sleep) {
            sleep = 1;
            delay(500);
        } else {
            sleep = 0;
            delay(500);
        }
    } 
}

// Time Function
void syncTime(char* data){
    char *token;
    int arr[6];
    int i=0;
    /* the string pointed to by string is broken up into the tokens
    "a string", " of", " ", and "tokens" ; the null terminator (\0)
    is encountered and execution stops after the token "tokens" */
    token = strtok(data, ":");
    while (token != NULL){
        Serial.print(token);
        arr[i] = atoi(token), i++; 
        token = strtok(NULL, ":");
    }
    
    setTime(arr[3], arr[4], arr[5], arr[2], arr[1], arr[0]);
    // setTime(Hour, Min, Sec, Day, Month, Year);          
}

void motor(void){
    int cnt = 2;
    for(int i=0; i<2; i++){
        digitalWrite(MOTOR, LOW);
        delay(50);
        digitalWrite(MOTOR, HIGH);
        delay(50);
    }
}

// Draw Function
void drawString(int x, int y, char* str){
    // graphic commands to redraw the complete screen should be placed here  
    u8g.setFont(u8g_font_unifont);
    // u8g.setFont(u8g_font_osb21);
    u8g.drawStr(x, y, str);
}

void drawSMSMessage(char* msg){
    int i=0;
    motor();
    drawImage((128-64)/2, 0, 8, 64, SMS);        
    delay(2500);
    
    while(1){
        if(!digitalRead(SW))
            break;
        else if(i >= 200) // 10 sec Wait
            break;
            
        u8g.firstPage();  
        do {
            drawString(0, 10, "SMS Message");
            drawString(0, 25, msg);
        } while(u8g.nextPage());
        // rebuild the picture after some delay
        delay(50);
        i++;
    } 
}

void drawIncomingCall(char* msg){
    drawImage((128-64)/2, 0, 8, 64, PHONE);        
    delay(2500);
    while(callObserver()){
        motor();   
        u8g.firstPage();  
        do {
            drawString(0, 10, "Incoming Call");
            drawString(0, 25, msg);
        } while(u8g.nextPage());
        // rebuild the picture after some delay
        delay(50);
    }
}

void drawImage(int x, int y, int cnt, int h, uint8_t* bitmap){
    // void U8GLIB::drawBitmap(u8g_uint_t x, u8g_uint_t y, u8g_uint_t cnt, u8g_uint_t h, const uint8_t *bitmap) 
    // void U8GLIB::drawBitmapP(u8g_uint_t x, u8g_uint_t y, u8g_uint_t cnt, u8g_uint_t h, const u8g_pgm_uint8_t *bitmap)
    /*
     * u8g: u8g구조체의 포인터 (C 인터페이스 전용).
     * x: OLED Xpos (비트 맵의 ​​왼쪽 위치).
     * y: OLED Ypos (비트 맵의 ​​위쪽).
     * cnt: 가로 방향 비트 맵 배열의 ​​바이트 수. 비트 맵 배열의 ​​높이는 cnt*8.
     * h: 비트 맵 배열의 ​​높이.
     */
    u8g.firstPage();  
    do { 
        u8g.drawBitmapP(x, y, cnt, h, bitmap);
    } while(u8g.nextPage());
    delay(50);
}

void drawAnalogClock(){
    // graphic commands to redraw the complete screen should be placed here  
    u8g.setFont(u8g_font_unifont);
    // CLOCK_STYLE_SIMPLE_ANALOG.
    u8g.drawCircle(xPos, yPos, rad);
    showTimePin(xPos, yPos, 0.1, 0.5, hour()*5 + (int)(minute()*5/60));
    showTimePin(xPos, yPos, 0.1, 0.65, minute());
    showTimePin(xPos, yPos, 0.1, 0.78, second());
}

// Calculate clock pin position
const double RAD = 3.141592/180;
const double LR = 89.99;
void showTimePin(int center_x, int center_y, double pl1, double pl2, double pl3){
    double x1, x2, y1, y2;
    x1 = xPos + (rad * pl1) * cos((6 * pl3 + LR) * RAD);
    y1 = yPos + (rad * pl1) * sin((6 * pl3 + LR) * RAD);
    x2 = xPos + (rad * pl2) * cos((6 * pl3 - LR) * RAD);
    y2 = yPos + (rad * pl2) * sin((6 * pl3 - LR) * RAD);
    
    u8g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
}
