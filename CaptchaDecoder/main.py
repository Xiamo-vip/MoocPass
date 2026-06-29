import base64
import urllib3
from flask import Flask, request, jsonify
from decoders import DecoderFactory
from services.chaoxing_service import ChaoxingCaptchaService

# 禁用 urllib3 未验证 HTTPS 请求的警告输出
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

app = Flask(__name__)



@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "service": "CaptchaDecoder",
        "available_decoders": list(DecoderFactory._registry.keys())
    })


@app.route('/ocr', methods=['POST'])
def ocr_endpoint():
    """
    通用验证码识别接口
    可传入 query param 或 json 字段 'type' (默认 'ddddocr')
    """
    try:
        decoder_type = request.args.get('type')
        data = request.get_json(silent=True) or {}
        if not decoder_type:
            decoder_type = data.get('type', 'ddddocr')

        decoder = DecoderFactory.get_decoder(decoder_type)
        if not decoder:
            return jsonify({
                "code": 404,
                "message": f"Decoder type '{decoder_type}' is not registered. Available: {list(DecoderFactory._registry.keys())}"
            }), 404

        img_bytes = None
        if 'image_base64' in data:
            img_bytes = base64.b64decode(data['image_base64'])
        elif request.data:
            img_bytes = request.data
        elif 'file' in request.files:
            img_bytes = request.files['file'].read()

        if not img_bytes:
            return jsonify({"code": 400, "message": "No image data provided"}), 400

        extra_params = data.get('extra_params')
        result = decoder.decode(img_bytes, extra_params=extra_params)
        return jsonify({"code": 0, "decoder": decoder.decoder_type, "result": result})
    except Exception as e:
        return jsonify({"code": 500, "message": str(e)}), 500


@app.route('/solve', methods=['POST'])
def solve_endpoint():
    """
    完整代办解决接口
    支持 json 参数：platform (默认 'chaoxing'), type (默认 'ddddocr'), cookies, user_agent
    """
    try:
        data = request.get_json(force=True)
        platform = data.get('platform', 'chaoxing').lower()
        decoder_type = data.get('type', 'ddddocr')
        cookies = data.get('cookies', '')
        user_agent = data.get('user_agent', '')

        if not cookies:
            return jsonify({"code": 400, "message": "cookies is required"}), 400

        if platform == 'chaoxing':
            success, msg_or_code = ChaoxingCaptchaService.try_pass(
                user_agent=user_agent, cookies=cookies, decoder_type=decoder_type
            )
            return jsonify({"code": 0, "success": success, "detail": msg_or_code})
        else:
            return jsonify({"code": 400, "message": f"Unsupported platform '{platform}'"}), 400
    except Exception as e:
        return jsonify({"code": 500, "message": str(e)}), 500


if __name__ == '__main__':
    print("[ServerStart] CaptchaDecoder modularized service running on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=False)