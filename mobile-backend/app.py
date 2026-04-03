import os
import sys

from flask import Flask, jsonify, request
from dotenv import load_dotenv

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.insert(0, ROOT)

load_dotenv(os.path.join(ROOT, '.env'))

from handlers.ai_handler import handle_query
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

@app.route('/ai', methods=['POST'])
def ai_query():
    data = request.get_json(silent=True) or {}
    query = data.get('query', '').strip()
    if not query:
        return jsonify({'error': 'Query is required.'}), 400

    answer = handle_query(query)
    return jsonify({'answer': answer})

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
