package rpc

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"
)

type Client struct {
	Endpoint string
	Username string
	Password string
	client   *http.Client
}

func NewClient(endpoint, username, password string) *Client {
	return &Client{
		Endpoint: endpoint,
		Username: username,
		Password: password,
		client:   &http.Client{Timeout: 30 * time.Second},
	}
}

type rpcRequest struct {
	Jsonrpc string      `json:"jsonrpc"`
	ID      string      `json:"id"`
	Method  string      `json:"method"`
	Params  interface{} `json:"params,omitempty"`
}

type rpcResponse struct {
	Jsonrpc string           `json:"jsonrpc"`
	ID      string           `json:"id"`
	Result  *json.RawMessage `json:"result,omitempty"`
	Error   *struct {
		Code    int    `json:"code"`
		Message string `json:"message"`
	} `json:"error,omitempty"`
}

// Call sends a JSON-RPC request and unmarshals the result into the provided result pointer.
func (c *Client) Call(ctx context.Context, method string, params interface{}, result interface{}) error {
	reqBody, err := json.Marshal(rpcRequest{
		Jsonrpc: "2.0",
		ID:      "0",
		Method:  method,
		Params:  params,
	})
	if err != nil {
		log.Printf("[RPC] Error marshaling request: %v", err)
		return err
	}

	if ctx == nil {
		ctx = context.Background()
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.Endpoint, bytes.NewBuffer(reqBody))
	if err != nil {
		log.Printf("[RPC] Error creating HTTP request: %v", err)
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	if c.Username != "" || c.Password != "" {
		req.SetBasicAuth(c.Username, c.Password)
	}

	resp, err := c.client.Do(req)
	if err != nil {
		log.Printf("[RPC] HTTP request error: %v", err)
		return err
	}
	defer func() {
		_, _ = io.Copy(io.Discard, resp.Body)
		_ = resp.Body.Close()
	}()

	var rpcResp rpcResponse
	if err := json.NewDecoder(resp.Body).Decode(&rpcResp); err != nil {
		log.Printf("[RPC] Error decoding response: %v", err)
		return err
	}

	if rpcResp.Error != nil {
		log.Printf("[RPC] RPC error: %d %s", rpcResp.Error.Code, rpcResp.Error.Message)
		return fmt.Errorf("RPC error %d: %s", rpcResp.Error.Code, rpcResp.Error.Message)
	}
	if result != nil && rpcResp.Result != nil {
		if err := json.Unmarshal(*rpcResp.Result, result); err != nil {
			log.Printf("[RPC] Error unmarshaling result: %v", err)
			return err
		}
	}
	return nil
}
