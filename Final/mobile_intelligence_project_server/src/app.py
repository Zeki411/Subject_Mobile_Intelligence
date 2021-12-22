import numpy as np
import json
from flask import Flask, request, jsonify, render_template, Response
import pickle
import os
import time

app = Flask(__name__)
act_gender_model = pickle.load(open('model_act_gender.pkl', 'rb'))

def format_server_time():
  server_time = time.localtime()
  return time.strftime("%I:%M:%S %p", server_time)

@app.route('/')
def index():
    context = { 'server_time': format_server_time() }
    return render_template('index.html', context=context)

@app.route('/test', methods=['POST'])
def get_data():
    print('Recieved from client: {}'.format(request.data))
    return Response('We recieved something…')

@app.route('/results',methods=['POST'])
def results():
    data = request.get_json(force=True)

    act_gender_prediction = act_gender_model.predict(np.array(data["data"]).reshape((-1,128,12)))
    rs = np.int(np.argmax(act_gender_prediction, axis=1))
    if(rs == 0):
        js_result =  {"act"    : "Walking",
                      "gender" : "Female" ,}
    if(rs == 1):
        js_result =  {"act"    : "Jogging",
                      "gender" : "female" ,}
    if(rs == 2):
        js_result =  {"act"    : "Standing",
                      "gender" : "None" ,}
    if(rs == 3):
        js_result =  {"act"    : "Walking",
                      "gender" : "Male" ,}
    if(rs == 4):
        js_result =  {"act"    : "Jogging",
                      "gender" : "Male" ,}
    print(js_result)
    return jsonify(js_result)

if __name__ == '__main__':
    app.run(debug=True,threaded=True,host='0.0.0.0',port=int(os.environ.get('PORT', 8080)))